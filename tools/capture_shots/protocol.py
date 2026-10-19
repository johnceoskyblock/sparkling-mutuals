"""Strict reader for the checked capture-shot protocol."""
from dataclasses import dataclass
from pathlib import Path
from types import MappingProxyType
from collections.abc import Mapping
import hashlib
import json
import math
import re

MAX_BYTES=10*1024*1024
MAX_LINE=64*1024
MAX_EVENTS=100_000
SCHEMA_PATH=Path(__file__).resolve().parents[2]/'docs/schemas/capture-shots-v1.schema.json'
SCHEMA=json.loads(SCHEMA_PATH.read_text(encoding='utf-8'))
SCHEMAS={1:SCHEMA,2:json.loads(SCHEMA_PATH.with_name('capture-shots-v2.schema.json').read_text(encoding='utf-8'))}

class ProtocolError(ValueError):
    pass

def freeze(value):
    if isinstance(value,dict):return MappingProxyType({k:freeze(v) for k,v in value.items()})
    if isinstance(value,list):return tuple(freeze(v) for v in value)
    return value

def plain(value):
    if isinstance(value,Mapping):return {k:plain(v) for k,v in value.items()}
    if isinstance(value,(list,tuple)):return [plain(v) for v in value]
    return value

def _validate(value,schema,root):
    if '$ref' in schema:
        ref=schema['$ref']
        if not ref.startswith('#/$defs/'):raise ProtocolError('unsupported schema reference')
        return _validate(value,root['$defs'][ref.split('/')[-1]],root)
    for keyword in ('oneOf','anyOf'):
        if keyword in schema:
            matches=0
            for option in schema[keyword]:
                try:_validate(value,option,root);matches+=1
                except ProtocolError:pass
            if matches<1 or keyword=='oneOf' and matches!=1:raise ProtocolError('schema variant/fields mismatch')
    def equal(a,b):return a==b and (type(a) is bool)==(type(b) is bool)
    if 'const' in schema and not equal(value,schema['const']):raise ProtocolError('schema constant mismatch')
    if 'enum' in schema and not any(equal(value,v) for v in schema['enum']):raise ProtocolError('unknown enum')
    kind=schema.get('type')
    types={'object':lambda v:isinstance(v,dict),'array':lambda v:isinstance(v,list),'string':lambda v:isinstance(v,str),
           'integer':lambda v:type(v) is int,'number':lambda v:type(v) in (int,float) and math.isfinite(v),
           'boolean':lambda v:type(v) is bool,'null':lambda v:v is None}
    if kind and not any(types[k](value) for k in ([kind] if isinstance(kind,str) else kind)):raise ProtocolError('schema type mismatch')
    if type(value) in (int,float):
        if not math.isfinite(value):raise ProtocolError('nonfinite number')
        if 'minimum' in schema and value<schema['minimum']:raise ProtocolError('below minimum')
        if 'maximum' in schema and value>schema['maximum']:raise ProtocolError('above maximum')
    if isinstance(value,str):
        if len(value)>schema.get('maxLength',65536):raise ProtocolError('string bound')
        if 'pattern' in schema and not re.fullmatch(schema['pattern'],value):raise ProtocolError('invalid token/pattern')
    if isinstance(value,list):
        if len(value)>schema.get('maxItems',MAX_EVENTS):raise ProtocolError('array bound')
        for item in value:_validate(item,schema.get('items',{}),root)
    if isinstance(value,dict):
        props=schema.get('properties',{})
        if not set(schema.get('required',())).issubset(value):raise ProtocolError('missing field')
        if schema.get('additionalProperties') is False and not set(value).issubset(props):raise ProtocolError('unknown/privacy field')
        for k,v in value.items():
            if k in props:_validate(v,props[k],root)

def _pairs(items):
    result={}
    for key,value in items:
        if key in result:raise ProtocolError('duplicate JSON key')
        result[key]=value
    return result

def _constant(value):raise ProtocolError('nonfinite JSON constant')

def _integer(value):
    if len(value.lstrip('-'))>19:raise ProtocolError('integer bound')
    return int(value)

@dataclass(frozen=True)
class Event:
    sequence: int
    micros: int
    tick: int
    kind: str
    data: dict

@dataclass(frozen=True)
class Shot:
    token: str
    input: Event
    tracks: tuple
    association: str
    feedback: tuple
    annotations: tuple

@dataclass(frozen=True)
class Track:
    token: str
    generation: int
    variant: str | None
    species: str | None
    events: tuple

@dataclass(frozen=True)
class Session:
    header: dict
    events: tuple
    footer: dict | None
    complete: bool
    issues: tuple
    input_sha256: str
    input_path: Path
    shots: tuple = ()
    tracks: tuple = ()

def read_session(path: Path) -> Session:
    path=Path(path).resolve(strict=True)
    if path.stat().st_size>MAX_BYTES:raise ProtocolError('file byte limit')
    header=None;footer=None;events=[];issues=[];total=0;sha=hashlib.sha256();sequence=0;micros=0;tick=0
    inputs={};entities={};closed=set();references=set();before_footer=0;by_token={};probes=set();version=None
    with path.open('rb') as source:
        while True:
            raw=source.readline(MAX_LINE+1)
            if not raw:break
            total+=len(raw);sha.update(raw)
            if total>MAX_BYTES:raise ProtocolError('file byte limit')
            if len(raw)>MAX_LINE:raise ProtocolError('line byte limit')
            if footer is not None:raise ProtocolError('record after footer')
            partial=not raw.endswith(b'\n')
            try:
                r=json.loads(raw.decode('utf-8'),object_pairs_hook=_pairs,parse_constant=_constant,parse_int=_integer)
            except (UnicodeError,json.JSONDecodeError,RecursionError) as e:
                if partial:issues.append('partial_tail');break
                raise ProtocolError('malformed complete line') from e
            if partial:issues.append('partial_tail')
            if version is None:
                if not isinstance(r,dict) or not isinstance(r.get('metadata'),dict):raise ProtocolError('missing versioned header')
                version=r['metadata'].get('schemaVersion')
            validate_record(r,version)
            if header is None:
                if r['type']!='header':raise ProtocolError('first record must be header')
                header=r
                limits=r['limits']
                for key,maximum in (('maxInputs',300),('maxEvents',MAX_EVENTS),('maxBytes',MAX_BYTES),('bufferBytes',4*1024*1024)):
                    if not 1<=limits[key]<=maximum:raise ProtocolError('invalid declared limits')
                if limits['footerReserve']!=65536:raise ProtocolError('invalid footer reserve')
                continue
            if r['type']=='header':raise ProtocolError('duplicate header')
            if r['sequence']<=sequence or r['micros']<micros or r['tick']<tick:raise ProtocolError('sequence/time order')
            sequence=r['sequence'];micros=r['micros'];tick=r['tick']
            if r['type']=='footer':
                footer=r;before_footer=total-len(raw);continue
            if len(events)>=min(MAX_EVENTS-1,header['limits']['maxEvents']-1):raise ProtocolError('event limit')
            kind=r['kind'];d=r['data'];token=d.get('token')
            if kind=='input':
                if token in inputs:raise ProtocolError('reused shot token')
                if len(inputs)>=header['limits']['maxInputs']:raise ProtocolError('input limit')
                inputs[token]=r
                if d['pose']['crosshair'] and d['pose']['crosshair']['target']:references.add(d['pose']['crosshair']['target'])
            elif kind in ('continuation','annotation'):
                if token not in inputs:raise ProtocolError('unknown shot token')
            elif kind in ('capsule','target'):
                if token in closed:raise ProtocolError('reused closed entity token')
                previous=entities.get(token)
                if previous and (previous['generation']!=d['generation'] or previous.get('variant')!=d.get('variant') or previous.get('species')!=d.get('species')):raise ProtocolError('entity generation/identity changed')
                if previous and kind=='capsule' and d['action']=='discovered':raise ProtocolError('duplicate entity discovery')
                entities[token]=d
                if kind=='capsule' and d['action']=='removed':closed.add(token)
                if d.get('spawnMicros') is not None and d['spawnMicros']>micros:raise ProtocolError('future spawn time')
            elif kind in ('packet','display_state'):
                references.add(token)
                if token in closed:raise ProtocolError('observation after entity removal')
                if kind=='packet' and d['observedMicros'] is not None and d['observedMicros']>micros:raise ProtocolError('future packet arrival')
            elif kind=='obstruction':
                if d['track']:references.add(d['track'])
                if d['shot'] and d['shot'] not in inputs:raise ProtocolError('unknown obstruction shot')
            elif kind=='contact_probe':
                p=d['probe'];shot=p['shot']
                if shot not in inputs or shot in probes:raise ProtocolError('unknown or duplicate probe shot')
                if inputs[shot]['data']['variant']!='ordinary' or inputs[shot]['data']['hand']!='main':raise ProtocolError('unsupported probe input')
                probes.add(shot)
                keys=[t['key'] for t in p['targets']]
                if len(set(keys))!=len(keys):raise ProtocolError('duplicate proxy token')
                primary=p['primary']
                if primary and primary['targetKey'] not in keys and primary['targetKey'] is not None and not p['truncated']:raise ProtocolError('unknown primary proxy')
                if any(b['parameter']<=a['parameter'] for a,b in zip(p['nodes'],p['nodes'][1:])):raise ProtocolError('probe parameter order')
            event=Event(r['sequence'],r['micros'],r['tick'],kind,freeze(d));events.append(event)
            if token:by_token.setdefault(token,[]).append(event)
    if header is None:raise ProtocolError('missing header')
    # A crosshair can outlive the bounded pre-input ring; missing context remains explicit rather than fabricated.
    if references-set(entities):issues.append('missing_entity_context')
    if total>header['limits']['maxBytes']:raise ProtocolError('declared byte limit')
    if footer is None:issues.append('missing_footer')
    else:
        if footer['eventCount']!=len(events) or footer['inputCount']!=len(inputs) or footer['encodedBytesBeforeFooter']!=before_footer:raise ProtocolError('footer counts/bytes mismatch')
        if not footer['complete']:issues.append('producer_incomplete')
        if footer['gaps']:issues.append('observation_gaps')
        if footer['complete'] and (footer['gaps'] or footer['reason'] not in ('user_stop','export')):raise ProtocolError('invalid complete footer')
    summaries={}
    for summary in footer['shots'] if footer else ():
        token=summary['token']
        if token not in inputs or token in summaries:raise ProtocolError('footer shot references')
        if any(t not in entities or entities[t].get('variant')!=summary['variant'] for t in summary['tracks']):raise ProtocolError('footer track references')
        if summary['variant']!=inputs[token]['data']['variant']:raise ProtocolError('footer variant mismatch')
        summaries[token]=summary
    shots=[]
    for token,r in inputs.items():
        summary=summaries.get(token,{})
        annotations=tuple(e.data['outcome'] for e in by_token[token] if e.kind=='annotation')
        shots.append(Shot(token,by_token[token][0],tuple(summary.get('tracks',())),summary.get('association','unmatched'),tuple(summary.get('feedback',())),annotations))
    tracks=tuple(Track(token,d['generation'],d.get('variant'),d.get('species'),tuple(by_token.get(token,()))) for token,d in entities.items())
    return Session(freeze(header),tuple(events),freeze(footer) if footer else None,not issues,tuple(issues),sha.hexdigest(),path,tuple(shots),tracks)

def validate_record(value: dict, schema_version: int) -> None:
    if type(schema_version) is not int or schema_version not in SCHEMAS:raise ProtocolError('unknown schema version')
    schema=SCHEMAS[schema_version]
    _validate(value,schema,schema)
