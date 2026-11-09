import {it,expect} from 'vitest';
import {Room,attachment} from '../src/room';
import {route} from '../src/routing';
import {Allocation} from '../src/allocator';
const a='1'.repeat(32),b='2'.repeat(32);
function setup(){let q:any={version:1,lobby:'m1',self:a,roster:[a,b],nonce:a,unusableMs:0,action:'check'};let r=route(undefined,q,0);r=route(r.room,{...q,self:b,nonce:b},1);r=route(r.room,{...q,unusableMs:30000},30000);
 const x=attachment(r.generation,a,a,[a,b],0),y=attachment(r.generation,b,b,[a,b],0);return {room:new Room(r.room,new Allocation([a,b]),[x,y]),x,y};}
function send(r:Room,s:ReturnType<typeof attachment>,kind:string,payload:any,extra={}){return r.receive(s,JSON.stringify({sequence:s.sequence+1,kind,payload:typeof payload==='string'?payload:JSON.stringify(payload),...extra}),100);}
const f=(r:Room,s:ReturnType<typeof attachment>,action:string,rest={})=>send(r,s,'F',{version:1,action,...rest});
it('missing cohort lease never releases barrier, but positive retirement does',()=>{
 const {room,x}=setup();f(room,x,'begin');f(room,x,'done');expect(room.ready).toBe(false);
 room.route.members[b].renewed=-1000000;expect(room.ready).toBe(false);room.route.members[b].retired=true;expect(room.ready).toBe(true);
});
it('validates bootstrap transaction before importing facts, never transfers holdings or prior positives',()=>{
 const {room,x,y}=setup();f(room,x,'begin');const facts=Array(38).fill(0);facts[0]=1;
 expect(()=>f(room,x,'facts',{reporter:b,nests:0,facts,caught:[0,0]})).toThrow();expect(room.route.members[a].completed).toBe(false);
 facts[0]=0;facts[23]=2;f(room,x,'facts',{reporter:b,nests:4,facts,caught:[1,0]});expect(room.union()).toEqual([0,0,0,0]);
 f(room,x,'done');expect(room.union()).toEqual([0,0,1,0]);expect(room.replay(y).some((v:any)=>v.type==='sparklings')).toBe(false);
});
it('late/reconnected participant gets no atomic snapshot until its own bootstrap completes',()=>{
 const {room,x,y}=setup();f(room,x,'begin');f(room,x,'done');f(room,y,'begin');f(room,y,'done');
 const fresh=attachment(room.route.generation,b,b,[a,b],0);room.sessions=[x,fresh];expect(room.replay(fresh).some((v:any)=>v.type==='sparklings')).toBe(false);
});
it('registered late joiner outside the fixed cohort must reconcile before a new allocation',()=>{
 const {room,x}=setup();room.route.cohort=[a];f(room,x,'begin');f(room,x,'done');
 room.allocation.hello(a,{display:true,ticketed:true,leader:true,present:[a,b],locked:{}},0);room.allocation.report(a,'FOREST',0);
 expect(room.allocation.allocate(5000,room.ready)).toBe(false);expect(room.allocation.locks).toEqual({});
});
it('bootstrap propagates a prior sticky conflict to the whole backup room',()=>{
 const {room,x,y}=setup();f(room,x,'begin');f(room,x,'locks',{locked:{[a]:'ICY'},instructed:{},conflict:true});f(room,x,'done');f(room,y,'begin');f(room,y,'done');
 expect(room.allocation.conflict).toBe(true);expect(room.ready).toBe(false);expect(room.allocation.snapshot(5000,a).notifications).toEqual([]);
});
it('enforces 4096 bytes, strict sequence and 40 messages per 10 seconds',()=>{
 const {room,x}=setup();f(room,x,'begin');f(room,x,'done');
 expect(()=>room.receive(x,JSON.stringify({sequence:'4',kind:'N',payload:'1'}),100)).toThrow();
 expect(()=>send(room,x,'N','1'.repeat(4096))).toThrow();
 x.rate=[];for(let i=0;i<40;i++)send(room,x,'N','1');expect(()=>send(room,x,'N','1')).toThrow();
});
