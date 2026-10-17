"""Validated MongoDB access for the CS 340 shelter dashboard."""
import logging
import math
import os
import re
from dataclasses import dataclass
from bson import ObjectId
from pymongo import MongoClient, ASCENDING
from pymongo.errors import PyMongoError, DuplicateKeyError

log = logging.getLogger(__name__)
class ValidationError(ValueError): pass
class DatabaseError(RuntimeError): pass

BREEDS = {
    'water': 'Labrador Retriever Mix|Chesapeake Bay Retriever|Newfoundland',
    'mountain': 'German Shepherd|Alaskan Malamute|Old English Sheepdog|Siberian Husky|Rottweiler',
    'disaster': 'Doberman Pinscher|German Shepherd|Golden Retriever|Bloodhound|Rottweiler'
}
FIELDS = ('animal_id','animal_type','breed','name','sex_upon_outcome',
          'age_upon_outcome_in_weeks','location_lat','location_long')
SEX = {'Intact Female','Intact Male','Spayed Female','Neutered Male','Unknown'}
TYPES = {'Dog','Cat','Bird','Livestock','Other'}

def rescue_query(category='reset'):
    if category == 'reset': return {}
    if not isinstance(category,str) or category not in BREEDS: raise ValidationError('Unknown rescue filter')
    return {'animal_type':'Dog','breed':{'$regex':BREEDS[category],'$options':'i'},
            'sex_upon_outcome':'Intact Female' if category=='water' else 'Intact Male',
            'age_upon_outcome_in_weeks':{'$gte':20 if category=='disaster' else 26,
                                       '$lte':300 if category=='disaster' else 156}}

def validate_record(data, partial=False):
    if not isinstance(data,dict) or not data: raise ValidationError('Supply record fields')
    if set(data)-set(FIELDS): raise ValidationError('Unsupported record field')
    required={'animal_id','animal_type','breed','sex_upon_outcome','age_upon_outcome_in_weeks'}
    if not partial and not required <= set(data): raise ValidationError('Missing required record field')
    out=dict(data)
    for key in ('animal_id','animal_type','breed','name','sex_upon_outcome'):
        if key in out:
            if not isinstance(out[key],str) or not out[key].strip() or len(out[key])>200:
                raise ValidationError('Invalid text field: '+key)
            out[key]=out[key].strip()
    if 'animal_id' in out and not re.fullmatch(r'[A-Za-z0-9_-]{1,40}',out['animal_id']):
        raise ValidationError('Invalid animal identifier')
    if 'animal_type' in out and out['animal_type'] not in TYPES: raise ValidationError('Invalid animal type')
    if 'sex_upon_outcome' in out and out['sex_upon_outcome'] not in SEX: raise ValidationError('Invalid sex value')
    for key,low,high in [('age_upon_outcome_in_weeks',0,2600),('location_lat',-90,90),('location_long',-180,180)]:
        if key in out:
            v=out[key]
            if isinstance(v,bool) or not isinstance(v,(int,float)) or not math.isfinite(v) or not low<=v<=high:
                raise ValidationError('Invalid numeric field: '+key)
    if ('location_lat' in out) != ('location_long' in out): raise ValidationError('Supply both coordinates together')
    return out

def record_id(value):
    if not isinstance(value,str) or not ObjectId.is_valid(value): raise ValidationError('Use a MongoDB record ID')
    return ObjectId(value)

@dataclass(frozen=True)
class Settings:
    uri: str
    database: str
    collection: str
    @classmethod
    def from_env(cls):
        uri=os.environ.get('MONGODB_URI','')
        db=os.environ.get('MONGODB_DATABASE','aac')
        col=os.environ.get('MONGODB_COLLECTION','animals')
        if not uri.startswith(('mongodb://','mongodb+srv://')): raise ValidationError('Set MONGODB_URI')
        if any(not re.fullmatch(r'[A-Za-z0-9_-]{1,64}',x) for x in (db,col)):
            raise ValidationError('Invalid database or collection name')
        return cls(uri,db,col)

class AnimalShelter:
    def __init__(self, collection):
        self.collection=collection
        self.client=None
    @classmethod
    def connect(cls):
        settings=Settings.from_env()
        client=None
        try:
            client=MongoClient(settings.uri,serverSelectionTimeoutMS=5000,connectTimeoutMS=5000)
            client.admin.command('ping')
        except PyMongoError:
            if client is not None: client.close()
            raise DatabaseError('Cannot connect to MongoDB; check settings and service') from None
        repo=cls(client[settings.database][settings.collection]);repo.client=client
        return repo
    def close(self):
        if self.client is not None: self.client.close()
    def _call(self,operation,fn):
        try:
            result=fn()
            log.info('database_operation',extra={'operation':operation,'outcome':'success'})
            return result
        except DuplicateKeyError: raise ValidationError('Duplicate database key') from None
        except PyMongoError:
            log.error('database_operation',extra={'operation':operation,'outcome':'failed'})
            raise DatabaseError('Database operation failed: '+operation) from None
    def create(self,data):
        valid=validate_record(data)
        return self._call('create',lambda:str(self.collection.insert_one(valid).inserted_id))
    def update(self,identifier,changes):
        oid=record_id(identifier);valid=validate_record(changes,partial=True)
        return self._call('update',lambda:self.collection.update_one({'_id':oid},{'$set':valid}).modified_count)
    def delete(self,identifier):
        oid=record_id(identifier)
        return self._call('delete',lambda:self.collection.delete_one({'_id':oid}).deleted_count)
    def read(self,category='reset',page=0,page_size=10):
        if type(page) is not int or not 0<=page<=10000: raise ValidationError('Invalid page')
        if type(page_size) is not int or not 1<=page_size<=100: raise ValidationError('Invalid page size')
        q=rescue_query(category)
        def operation():
            count=self.collection.count_documents(q,maxTimeMS=5000)
            cursor=self.collection.find(q,{k:1 for k in FIELDS}).sort('_id',ASCENDING).skip(page*page_size).limit(page_size).max_time_ms(5000)
            rows=[]
            for doc in cursor:
                doc['id']=str(doc.pop('_id'));rows.append(doc)
            return rows,count
        return self._call('read',operation)
    def breed_summary(self,category='reset'):
        pipeline=[{'$match':rescue_query(category)},{'$group':{'_id':'$breed','count':{'$sum':1}}},
                  {'$sort':{'count':-1,'_id':1}},{'$limit':20}]
        return self._call('aggregate',lambda:list(self.collection.aggregate(pipeline,maxTimeMS=5000)))
    def rescue_summary(self):
        # One server-side aggregation, overlapping rescue categories are intentional.
        facets={name:[{'$match':rescue_query(name)},{'$count':'count'}] for name in BREEDS}
        result=self._call('rescue_summary',lambda:list(self.collection.aggregate([{'$facet':facets}],maxTimeMS=5000)))
        return {name:(result[0][name][0]['count'] if result and result[0][name] else 0) for name in BREEDS}
    def create_indexes(self):
        # Regex breeds remain residual filters; do not claim regex uses this index.
        return self._call('index',lambda:self.collection.create_index(
            [('animal_type',1),('sex_upon_outcome',1),('age_upon_outcome_in_weeks',1)],name='rescue_type_sex_age'))

def map_record(rows,selected_ids):
    """Resolve a stable ID in the displayed page, never a positional column number."""
    if not rows:return None
    row=next((r for r in rows if selected_ids and r['id']==selected_ids[0]),rows[0])
    lat,lon=row.get('location_lat'),row.get('location_long')
    if any(isinstance(v,bool) or not isinstance(v,(int,float)) or not math.isfinite(v) for v in (lat,lon)):return None
    if not -90<=lat<=90 or not -180<=lon<=180:return None
    return {'lat':lat,'lon':lon,'name':row.get('name','Unnamed'),'breed':row.get('breed','Unknown')}

def dashboard_data(repo,category,page):
    rows,total=repo.read(category,page,10)
    return rows,math.ceil(total/10),repo.breed_summary(category),total
