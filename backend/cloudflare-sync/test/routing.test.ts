import { describe, it, expect } from 'vitest';
import { route, type RouteRequest, type RouteRoom } from '../src/routing';
import { identity, numbers } from '../src/protocol';
const a='0'.repeat(31)+'1',b='0'.repeat(31)+'2',c='0'.repeat(31)+'3';
const req=(self=a, nonce='aaaaaaaaaaaaaaaa', extra:Partial<RouteRequest>={}):RouteRequest=>({version:1,lobby:'mini147AH',self,roster:[a,b],nonce,unusableMs:0,action:'check',...extra});
describe('routing generations',()=>{
 it('requires sustained failure and converges healthy peers without switching back',()=>{
  let r=route(undefined,req(),0); const generation=r.room.generation;
  r=route(r.room,req(a,undefined,{unusableMs:29999}),29999);expect(r.provider).toBe('primary');
  r=route(r.room,req(a,undefined,{unusableMs:30000}),30000);expect(r.provider).toBe('backup');
  r=route(r.room,req(b,'bbbbbbbbbbbbbbbb'),30001);expect(r.provider).toBe('backup');expect(r.room.generation).toBe(generation);
  expect(route(r.room,req(),30002).provider).toBe('backup');
 });
 it('captures a fixed cohort and lease expiry cannot silently retire it',()=>{
  let r=route(undefined,req(),0);r=route(r.room,req(b,'bbbbbbbbbbbbbbbb'),1);
  r=route(r.room,req(a,undefined,{unusableMs:30000}),30000);
  expect(r.room.cohort.sort()).toEqual([a,b]);
  r=route(r.room,req(),130000);expect(r.room.cohort).toContain(b);expect(r.room.members[b].retired).toBe(false);
 });
 it('holds a new visit nonce pending until everyone positively leaves',()=>{
  let r=route(undefined,req(),0);r=route(r.room,req(b,'bbbbbbbbbbbbbbbb'),1);
  const gen=r.room.generation;
  r=route(r.room,req(a,'cccccccccccccccc'),2);expect(r.admitted).toBe(false);expect(r.room.generation).toBe(gen);
  r=route(r.room,req(b,'bbbbbbbbbbbbbbbb',{action:'leave'}),3);
  r=route(r.room,req(a,'cccccccccccccccc'),4);expect(r.admitted).toBe(true);expect(r.room.generation).not.toBe(gen);expect(r.provider).toBe('primary');
 });
 it('expiry requires ten minutes without any renewed participant',()=>{
  const old=route(undefined,req(),0).room;
  expect(route(structuredClone(old),req(a,'cccccccccccccccc'),599999).admitted).toBe(false);
  expect(route(structuredClone(old),req(a,'cccccccccccccccc'),600000).room.generation).not.toBe(old.generation);
 });
 it('accepts only a confirmed new member expansion, never stale subset rollback',()=>{
  const old=route(undefined,req(),0).room;
  const expanded=route(old,req(c,'cccccccccccccccc',{roster:[a,b,c]}),1);
  expect(expanded.admitted).toBe(true);expect(expanded.room.generation).toBe(old.generation);
  expect(()=>route(expanded.room,req(),2)).toThrow();
  expect(()=>route(old,req(c,'cccccccccccccccc',{roster:[b,c]}),2)).toThrow();
 });
 it('old leave cannot retire a replacement generation',()=>{
  let r=route(undefined,req(),0);r=route(r.room,req(a,undefined,{action:'leave'}),1);
  r=route(r.room,req(a,'cccccccccccccccc'),2);const gen=r.room.generation;
  r=route(r.room,req(a,undefined,{action:'leave'}),3);expect(r.room.generation).toBe(gen);expect(r.room.members[a].retired).toBe(false);
 });
});
describe('strict wire identity and numeric bounds',()=>{
 it('canonicalizes case and mini aliases, rejects duplicate or foreign self',()=>{
  expect(identity('MINI147ah',a,[b,a]).lobby).toBe('m147ah');
  expect(()=>identity('m1',c,[a,b])).toThrow();expect(()=>identity('m1',a,[a,a])).toThrow();
 });
 it('preserves exact aggregate pairs and rejects numeric coercion',()=>{
  const v=Array(39).fill(0);v[17]=-1;v[33]=3;expect(numbers('A',v.join(','))).toEqual(v);
  expect(()=>numbers('A',v.slice(0,38).join(','))).toThrow();v[33]=2;expect(numbers('A',v.slice(0,38).join(','))).toHaveLength(38);
  for(const p of ['1.0','1e0','8192','-1',' 1'])expect(()=>numbers('N',p)).toThrow();
  expect(()=>numbers('S','0,128,0,0')).toThrow();
 });
});
