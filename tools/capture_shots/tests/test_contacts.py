import copy
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from tools.capture_shots import contacts
from tools.capture_shots.protocol import read_session,ProtocolError,validate_record
from tools.capture_shots.replay import replay
from tools.capture_shots.tests.test_protocol import FIXTURES,records,encoded

class ContactsTest(unittest.TestCase):
    def read(self,items):
        self.path.write_bytes(encoded(items));return read_session(self.path)
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.path=Path(self.temp.name)/'input.jsonl'
    def test_real_writer_and_alternating_versions_have_independent_strict_schemas(self):
        for name in ('contact-probe-v2.jsonl','isolated.jsonl','contact-probe-v2.jsonl','ambiguous.jsonl'):
            s=read_session(FIXTURES/name);self.assertTrue(s.complete)
        report=contacts.summarize(read_session(FIXTURES/'contact-probe-v2.jsonl'))
        self.assertTrue(report['contact_prediction_available']);self.assertIsNone(report['shots'][0]['endpoint_error'])
        self.assertFalse(report['shots'][0]['observed_endpoint_available'])
    def test_v1_removal_and_no_message_never_become_impact_or_miss(self):
        report=contacts.summarize(read_session(FIXTURES/'isolated.jsonl'))
        self.assertFalse(report['contact_prediction_available']);self.assertIsNone(report['shots'][0]['endpoint_error'])
        self.assertFalse(report['shots'][0]['observed_endpoint_available']);self.assertEqual([],report['shots'][0]['typed_feedback'])
    def test_terrain_prediction_is_separate_from_captured_critter_and_raw_positions(self):
        r=records('contact-probe-v2.jsonl');p=r[2]['data']['probe']
        p['primary']={'kind':'terrain','parameter':.5,'position':{'x':0,'y':0,'z':.4},'normal':{'x':0,'y':0,'z':-1},'targetKey':None,'proxy':None};p['stop']='contact'
        s=self.read(r);report=contacts.summarize(s)
        self.assertEqual('terrain',report['shots'][0]['predicted_contact']['kind']);self.assertIsNone(report['shots'][0]['endpoint_error'])
        result=replay(s);self.assertEqual(1,len(result.summary['contact_predictions']))
        self.assertFalse(any(e.kind=='contact_probe' for e in result.rows[0]))
    def test_probe_references_privacy_and_geometry_caps_are_strict(self):
        for change in ('shot','duplicate','key','privacy','targets','nodes','nan','version','v1','reason','generation'):
            r=records('contact-probe-v2.jsonl');p=r[2]['data']['probe']
            if change=='shot':p['shot']='unknown'
            if change=='duplicate':r.insert(3,copy.deepcopy(r[2]));r[3]['sequence']=3;r[-1]['sequence']=4
            if change=='key':p['targets'][0]['key']='runtime-identity'
            if change=='privacy':p['uuid']='private'
            if change=='targets':p['targets']*=65
            if change=='nodes':p['nodes']*=3
            if change=='nan':p['pitch']=float('nan')
            if change=='version':r[0]['metadata']['schemaVersion']=3
            if change=='v1':r[0]['metadata'].update(schemaVersion=1,recorderVersion=2)
            if change=='reason':p['coverage']['reasons']=['arbitrary-private']
            if change=='generation':p['targets'][0]['generation']=123456
            with self.subTest(change=change),self.assertRaises(ProtocolError):self.read(r)
    def test_truncated_probe_does_not_erase_raw_file_completeness_but_measurement_is_flagged(self):
        r=records('contact-probe-v2.jsonl');r[2]['data']['probe'].update(truncated=True,omittedTerrain=1)
        s=self.read(r);self.assertTrue(s.complete)
        self.assertTrue(contacts.summarize(s)['shots'][0]['probe_incomplete'])
    def test_create_new_reports_preserve_source_hash_and_reject_overwrite(self):
        source=FIXTURES/'contact-probe-v2.jsonl';before=hashlib.sha256(source.read_bytes()).hexdigest();s=read_session(source)
        output=Path(self.temp.name)/'out';contacts.write_report(s,output)
        self.assertEqual(before,json.loads((output/'contacts.json').read_text())['input_sha256'])
        with self.assertRaises(ValueError):contacts.write_report(s,output)
        self.assertEqual(before,hashlib.sha256(source.read_bytes()).hexdigest())
    def test_validate_record_rejects_bool_and_float_versions(self):
        for v in (True,2.0,0,3):
            with self.assertRaises(ProtocolError):validate_record(records()[0],v)
