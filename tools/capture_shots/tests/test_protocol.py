import copy
import json
from pathlib import Path
import tempfile
import unittest
from tools.capture_shots.protocol import read_session, ProtocolError

ROOT=Path(__file__).resolve().parents[3]
FIXTURES=ROOT/'src/test/resources/shots'

def records(name='isolated.jsonl'):
    return [json.loads(line) for line in (FIXTURES/name).read_text(encoding='utf-8').splitlines()]

def encoded(items):
    result=b''
    for item in copy.deepcopy(items):
        if item['type']=='footer':
            item['encodedBytesBeforeFooter']=len(result)
            item['eventCount']=sum(r['type']=='event' for r in items)
            item['inputCount']=sum(r.get('kind')=='input' for r in items)
        result+=(json.dumps(item,separators=(',',':'),ensure_ascii=False)+'\n').encode()
    return result

class ProtocolTest(unittest.TestCase):
    def test_real_kotlin_service_export_preserves_whole_path_provenance(self):
        s=read_session(FIXTURES/'service-export.jsonl')
        self.assertTrue(s.complete);self.assertEqual(9,len(s.events));self.assertEqual(1,len(s.shots))
        self.assertEqual(('unknown',),s.shots[0].annotations)
        packet=next(e for e in s.events if e.kind=='packet' and e.data['update']=='move')
        self.assertAlmostEqual(.25,packet.data['points'][0]['position']['x'])
        self.assertNotEqual(packet.data['points'][0]['position']['x'],packet.data['resolvedEnd']['x'])
        self.assertEqual('game',next(e for e in s.events if e.kind=='feedback').data['source'])
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.path=Path(self.temp.name)/'input.jsonl'
    def read(self,items):
        self.path.write_bytes(encoded(items));return read_session(self.path)
    def test_actual_producer_fixture_counts_and_unknown_outcomes(self):
        s=read_session(FIXTURES/'isolated.jsonl')
        self.assertTrue(s.complete);self.assertEqual(4,len(s.events));self.assertEqual('s1',s.shots[0].token)
        self.assertEqual(('c1',),s.shots[0].tracks);self.assertEqual('candidate_unique',s.shots[0].association)
        self.assertEqual((),s.shots[0].annotations)
    def test_partial_file_never_becomes_complete(self):
        s=read_session(FIXTURES/'partial.jsonl')
        self.assertFalse(s.complete);self.assertIn('missing_footer',s.issues)
        self.assertTrue(all(e.kind!='miss' for e in s.events))
    def test_truncated_utf8_tail_retains_complete_prior_lines(self):
        self.path.write_bytes((FIXTURES/'partial.jsonl').read_bytes()+b'{"broken":"\xe2\x82')
        s=read_session(self.path);self.assertFalse(s.complete);self.assertEqual(4,len(s.events));self.assertIn('partial_tail',s.issues)
    def test_duplicate_keys_nonfinite_and_malformed_interior_are_rejected(self):
        for raw in (b'{"type":"header","type":"header"}\n',b'{"value":NaN}\n',b'{"value":Infinity}\n',b'{"value":1e999}\n',b'not json\n'):
            with self.subTest(raw=raw):
                self.path.write_bytes(raw)
                with self.assertRaises(ProtocolError): read_session(self.path)
    def test_schema_versions_privacy_keys_and_token_paths_are_rejected(self):
        for change in ('version','privacy','path','enum'):
            r=records()
            if change=='version':r[0]['metadata']['schemaVersion']=2
            if change=='privacy':r[1]['data']['rawChat']='private'
            if change=='path':r[1]['data']['token']='../../outside'
            if change=='enum':r[1]['data']['hand']='third'
            with self.subTest(change=change),self.assertRaises(ProtocolError):self.read(r)
    def test_order_footer_counts_and_entity_lifetimes_are_checked(self):
        for change in ('sequence','clock','after_footer','reuse','generation'):
            r=records()
            if change=='sequence':r[2]['sequence']=r[1]['sequence']
            if change=='clock':r[3]['micros']=0
            if change=='after_footer':r.append(copy.deepcopy(r[3]))
            if change=='generation':
                event=copy.deepcopy(r[2]);event['sequence']=4;event['micros']=140000;event['data']['generation']=2;r.insert(-1,event);r[-1]['sequence']=5
            if change=='reuse':
                removed=copy.deepcopy(r[2]);removed['sequence']=4;removed['micros']=140000;removed['data'].update(action='removed',removal='server_remove')
                again=copy.deepcopy(r[2]);again['sequence']=5;again['micros']=150000;r[-1]['sequence']=6;r[-1:-1]=[removed,again]
            with self.subTest(change=change),self.assertRaises(ProtocolError):self.read(r)
    def test_footer_byte_count_is_not_trusted(self):
        r=records();data=encoded(r).replace(b'"eventCount":4',b'"eventCount":8');self.path.write_bytes(data)
        with self.assertRaisesRegex(ProtocolError,'footer'):read_session(self.path)
    def test_boolean_cannot_impersonate_schema_version_one(self):
        r=records();r[0]['metadata']['schemaVersion']=True
        with self.assertRaises(ProtocolError):self.read(r)
    def test_reader_enforces_line_file_and_event_caps(self):
        self.path.write_bytes(b' '*65536+b'\n')
        with self.assertRaisesRegex(ProtocolError,'line'):read_session(self.path)
        self.path.write_bytes(b' '*(10*1024*1024+1))
        with self.assertRaisesRegex(ProtocolError,'file'):read_session(self.path)
        r=records();r[0]['limits']['maxEvents']=2
        with self.assertRaisesRegex(ProtocolError,'event'):self.read(r)
    def test_shared_and_manual_outcome_provenance_stays_typed(self):
        r=records();r[-1].update(sequence=7,micros=800000,tick=16)
        r[-1:-1]=[{'type':'event','sequence':5,'micros':600000,'tick':12,'kind':'feedback','data':{'feedback':{'kind':'shared_capture','species':'Gemzie','variant':None},'dispatch':1,'source':'game'}},
                   {'type':'event','sequence':6,'micros':700000,'tick':14,'kind':'annotation','data':{'token':'s1','outcome':'miss'}}]
        s=self.read(r)
        self.assertEqual('shared_capture',s.events[-2].data['feedback']['kind']);self.assertEqual('annotation',s.events[-1].kind)

if __name__=='__main__':unittest.main()
