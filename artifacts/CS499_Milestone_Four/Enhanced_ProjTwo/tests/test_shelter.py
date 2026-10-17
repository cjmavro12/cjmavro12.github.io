import os
import unittest
from unittest.mock import patch
import mongomock
from pymongo.errors import OperationFailure
from animal_shelter import *
from demo_data import records
from dashboard import make_app

class ShelterTests(unittest.TestCase):
    def setUp(self):
        self.repo=AnimalShelter(mongomock.MongoClient().test.animals)
        for r in records():self.repo.create(r)
    def test_crud_one_record(self):
        row=next(records());row['name']='New';identifier=self.repo.create(row)
        self.assertEqual(self.repo.update(identifier,{'name':'Edited'}),1)
        self.assertEqual(self.repo.collection.find_one({'_id':ObjectId(identifier)})['name'],'Edited')
        self.assertEqual(self.repo.delete(identifier),1);self.assertEqual(self.repo.collection.count_documents({}),80)
    def test_no_bulk_mutations(self):
        for value in [{},{'$ne':None},'',None,'x']:
            with self.assertRaises(ValidationError):self.repo.delete(value)
            with self.assertRaises(ValidationError):self.repo.update(value,{'name':'Oops'})
        self.assertEqual(self.repo.collection.count_documents({}),80)
    def test_query_injection(self):
        row=next(records());row['breed']={'$ne':None}
        with self.assertRaises(ValidationError):self.repo.create(row)
        with self.assertRaises(ValidationError):self.repo.update(str(ObjectId()),{'$set':{'name':'x'}})
    def test_validation(self):
        for field,value in [('animal_id','!'),('animal_type','Tiger'),('breed',''),('sex_upon_outcome','x'),('age_upon_outcome_in_weeks',-1),('age_upon_outcome_in_weeks',float('nan')),('location_lat',91),('location_long',181),('location_lat',True)]:
            row=next(records());row[field]=value
            with self.assertRaises(ValidationError):self.repo.create(row)
        with self.assertRaises(ValidationError):self.repo.create({})
        with self.assertRaises(ValidationError):self.repo.update(str(ObjectId()),{'location_lat':30})
    def test_pagination(self):
        a,total=self.repo.read(page=0);b,_=self.repo.read(page=1)
        self.assertEqual(total,80);self.assertEqual(len(a),10)
        self.assertFalse(set(r['id'] for r in a)&set(r['id'] for r in b))
        for p,s in [(-1,10),(0,101),(True,10)]:
            with self.assertRaises(ValidationError):self.repo.read(page=p,page_size=s)
    def test_rescue_filters(self):
        for category in BREEDS:
            rows,total=self.repo.read(category,page_size=100)
            self.assertGreater(total,0)
            self.assertTrue(all(r['animal_type']=='Dog' for r in rows))
        with self.assertRaises(ValidationError):self.repo.read({'$where':'x'})
    def test_aggregations(self):
        self.assertEqual(sum(x['count'] for x in self.repo.breed_summary()),80)
        summary=self.repo.rescue_summary()
        for category in BREEDS:self.assertEqual(summary[category],self.repo.read(category)[1])
    def test_indexes(self):
        self.repo.create_indexes();self.repo.create_indexes()
        self.assertIn('rescue_type_sex_age',self.repo.collection.index_information())
    def test_map_named_fields(self):
        rows,_=self.repo.read();row=dict(reversed(list(rows[2].items())))
        result=map_record([row], [row['id']]);self.assertEqual(result['lat'],row['location_lat'])
        self.assertEqual(map_record(rows,[rows[2]['id']])['name'],rows[2]['name'])
        self.assertIsNone(map_record([],[]));row['location_lat']=None
        self.assertIsNone(map_record([row],[]))
    def test_callback_data(self):
        rows,pages,summary,total=dashboard_data(self.repo,'water',0)
        self.assertEqual(pages,2);self.assertEqual(total,20);self.assertEqual(len(rows),10)
        self.assertEqual(sum(x['count'] for x in summary),20)
    def test_database_errors_distinct_from_empty(self):
        with patch.object(self.repo.collection,'count_documents',side_effect=OperationFailure('private URI')):
            with self.assertRaises(DatabaseError) as cm:self.repo.read()
            self.assertNotIn('private',str(cm.exception))
    def test_settings(self):
        with patch.dict(os.environ,{},clear=True):
            with self.assertRaises(ValidationError):Settings.from_env()
        with patch.dict(os.environ,{'MONGODB_URI':'mongodb://localhost:27017','MONGODB_DATABASE':'aac','MONGODB_COLLECTION':'animals'},clear=True):
            self.assertEqual(Settings.from_env().database,'aac')
    def test_dash_routes_and_callback(self):
        app=make_app(self.repo,True);client=app.server.test_client()
        self.assertEqual(client.get('/').status_code,200)
        self.assertEqual(client.get('/_dash-layout').status_code,200)
        key=next(k for k in app.callback_map if 'animals.data' in k)
        outputs=[{'id':x.component_id,'property':x.component_property} for x in app.callback_map[key]['output']]
        response=client.post('/_dash-update-component',json={'output':key,'outputs':outputs,'inputs':[{'id':'category','property':'value','value':'water'},{'id':'page','property':'data','value':0}],'state':[],'changedPropIds':['category.value']})
        self.assertEqual(response.status_code,200,response.data)
        self.assertEqual(len(response.json['response']['animals']['data']),10)

if __name__=='__main__':unittest.main(verbosity=2)
