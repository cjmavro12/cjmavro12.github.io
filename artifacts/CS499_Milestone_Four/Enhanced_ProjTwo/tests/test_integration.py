"""Opt-in integration test. Uses and removes ONLY a new random test database."""
import os
import unittest
import uuid
from pymongo import MongoClient
from animal_shelter import AnimalShelter
from demo_data import records
from benchmark import measure

@unittest.skipUnless(os.environ.get('TEST_MONGODB_URI'), 'TEST_MONGODB_URI not set; live MongoDB unverified')
class LiveMongoTests(unittest.TestCase):
    def test_live_crud_aggregation_and_explain(self):
        client=MongoClient(os.environ['TEST_MONGODB_URI'],serverSelectionTimeoutMS=5000)
        name='cs499_test_'+uuid.uuid4().hex
        try:
            client.admin.command('ping')
            repo=AnimalShelter(client[name].animals)
            ids=[repo.create(r) for r in records()]
            self.assertEqual(repo.read()[1],80)
            self.assertEqual(repo.update(ids[0],{'name':'Updated'}),1)
            self.assertEqual(sum(x['count'] for x in repo.breed_summary()),80)
            repo.create_indexes()
            result=measure(repo)
            self.assertEqual(result['collection_scan']['nReturned'],result['indexed']['nReturned'])
            self.assertEqual(repo.delete(ids[0]),1)
        finally:
            client.drop_database(name)
            client.close()
