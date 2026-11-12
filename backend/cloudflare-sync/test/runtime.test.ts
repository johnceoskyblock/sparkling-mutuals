import {it,expect,afterEach} from 'vitest';
import {SELF,env,reset,evictDurableObject,runInDurableObject} from 'cloudflare:test';
import type {Env,Coordinator} from '../src/index';
const a='1'.repeat(32),b='2'.repeat(32);
afterEach(()=>reset());
async function auth(id:string){const r=await SELF.fetch('https://backup.test/v1/auth/uuid/session',{method:'POST',body:JSON.stringify({uuid:id})});expect(r.status).toBe(200);return (await r.json() as any).token;}
async function routing(id:string,token:string,unusableMs=30000){const r=await SELF.fetch('https://backup.test/v1/route',{method:'POST',headers:{Authorization:`Bearer ${token}`},body:JSON.stringify({version:1,lobby:'m123',self:id,roster:[a,b],nonce:id,action:'check',unusableMs})});expect(r.status).toBe(200);return r.json() as Promise<any>;}
async function socket(id:string,token:string,gen:string,roster=[a,b],transport=1){const r=await SELF.fetch('https://backup.test/v1/sync',{headers:{Upgrade:'websocket',Authorization:`Bearer ${token}`,'X-SafariUtils-Lobby':'m123','X-SafariUtils-Player':id,'X-SafariUtils-Roster':roster.join(','),'X-SM-Generation':gen,'X-SM-Visit':id,...(transport>1?{'X-SafariUtils-Sync-Protocol':String(transport)}:{})}});expect(r.status).toBe(101);const ws=r.webSocket!;ws.accept();const frames:any[]=[];ws.addEventListener('message',e=>{frames.push(JSON.parse(e.data as string));});return {ws,frames};}
async function flush(){await new Promise(resolve=>setTimeout(resolve,30));}
async function completionSocket(id:string,token:string,gen:string){
 const r=await SELF.fetch('https://backup.test/v1/completion-sync',{headers:{Upgrade:'websocket',Authorization:`Bearer ${token}`,'X-SafariUtils-Lobby':'m123','X-SafariUtils-Player':id,'X-SafariUtils-Roster':[a,b].join(','),'X-SM-Generation':gen,'X-SM-Visit':id}});
 expect(r.status).toBe(101);const ws=r.webSocket!;ws.accept();const frames:any[]=[];ws.addEventListener('message',e=>{frames.push(JSON.parse(e.data as string));});return {ws,frames};
}
it('completion channel works with healthy primary without entering or mutating main allocation',async()=>{
 const ta=await auth(a),tb=await auth(b);const r=await routing(a,ta,0);await routing(b,tb,0);expect(r.provider).toBe('primary');
 const x=await completionSocket(a,ta,r.generation),y=await completionSocket(b,tb,r.generation);
 const rows=Array.from({length:37},()=>[0,0,0]);rows[6]=[7,3,3];
 x.ws.send(JSON.stringify({sequence:1,kind:'C',payload:JSON.stringify({version:1,rows})}));
 await expect.poll(()=>y.frames.find(f=>f.kind==='C'),{timeout:2000,interval:10}).toBeDefined();
 expect(y.frames.find(f=>f.kind==='C').sender).toBe(a);
 const ns=(env as unknown as Env).COORDINATOR,stub=ns.get(ns.idFromName('coordinator-v1'));
 const state=await runInDurableObject(stub,(_instance:any,state)=>state.storage.sql.exec<{routing:string;allocation:string}>('SELECT routing,allocation FROM rooms WHERE id=?',r.generation).one());
 expect(JSON.parse(state.routing).provider).toBe('primary');expect(JSON.parse(state.allocation).locks).toEqual({});
 await evictDurableObject(stub);
 const z=await completionSocket(b,tb,r.generation);
 await expect.poll(()=>z.frames.find(f=>f.kind==='C'),{timeout:2000,interval:10}).toBeDefined();
 rows[6]=[0,0,0];x.ws.send(JSON.stringify({sequence:2,kind:'C',payload:JSON.stringify({version:1,rows})}));
 await expect.poll(()=>z.frames.find(f=>f.sequence===2),{timeout:2000,interval:10}).toBeDefined();
 expect(JSON.parse(z.frames.find(f=>f.sequence===2).payload).rows[6]).toEqual([0,0,0]);
 expect(z.frames.some(f=>['hotspots','sparklings','bootstrap-facts'].includes(f.type))).toBe(false);
 x.ws.close();y.ws.close();z.ws.close();
});
it('completion channel rejects foreign visits and retired members and cannot send main state',async()=>{
 const ta=await auth(a),tb=await auth(b);const r=await routing(a,ta,0);await routing(b,tb,0);
 const wrong=await SELF.fetch('https://backup.test/v1/completion-sync',{headers:{Upgrade:'websocket',Authorization:`Bearer ${ta}`,'X-SafariUtils-Lobby':'m124','X-SafariUtils-Player':a,'X-SafariUtils-Roster':[a,b].join(','),'X-SM-Generation':r.generation,'X-SM-Visit':a}});expect(wrong.status).toBe(400);
 const x=await completionSocket(a,ta,r.generation);let closed=false;x.ws.addEventListener('close',()=>closed=true);
 x.ws.send(JSON.stringify({sequence:1,kind:'S',payload:'1,0,0,0'}));await expect.poll(()=>closed,{timeout:2000,interval:10}).toBe(true);
 const y=await completionSocket(b,tb,r.generation);let retired=false;y.ws.addEventListener('close',()=>retired=true);
 await SELF.fetch('https://backup.test/v1/route',{method:'POST',headers:{Authorization:`Bearer ${tb}`},body:JSON.stringify({version:1,lobby:'m123',self:b,roster:[a,b],nonce:b,action:'leave',unusableMs:0})});
 y.ws.send(JSON.stringify({sequence:1,kind:'C',payload:JSON.stringify({version:1,rows:Array.from({length:37},()=>[7,3,3])})}));await expect.poll(()=>retired,{timeout:2000,interval:10}).toBe(true);
});
it('completion socket replacement and closing do not replace or disconnect the main backup socket',async()=>{
 const token=await auth(a),r=await routing(a,token),main=await socket(a,token,r.generation);
 const side=await completionSocket(a,token,r.generation),replacement=await completionSocket(a,token,r.generation);replacement.ws.close();
 main.ws.send(JSON.stringify({sequence:1,kind:'F',payload:'{"version":1,"action":"begin"}'}));
 main.ws.send(JSON.stringify({sequence:2,kind:'F',payload:'{"version":1,"action":"done"}'}));
 main.ws.send(JSON.stringify({sequence:3,kind:'N',payload:'1'}));
 await expect.poll(()=>main.frames.find(f=>f.kind==='N'),{timeout:2000,interval:10}).toBeDefined();
 const ns=(env as unknown as Env).COORDINATOR,stub=ns.get(ns.idFromName('coordinator-v1'));
 const mainCount=await runInDurableObject(stub,(_instance:any,state)=>state.getWebSockets().filter(ws=>{const s=ws.deserializeAttachment() as any;return s.self===a&&s.channel!=='completion';}).length);
 expect(mainCount).toBe(1);main.ws.close();side.ws.close();
});
it('real HTTP auth rejects foreign claims and isolates origin-scoped tokens',async()=>{
 expect((await SELF.fetch('https://backup.test/v1/route',{method:'POST',body:'{}'})).status).toBe(401);
 const token=await auth(a);expect((await SELF.fetch('https://other.test/v1/route',{method:'POST',headers:{Authorization:`Bearer ${token}`},body:'{}'})).status).toBe(401);
 expect((await SELF.fetch('https://backup.test/v1/route',{method:'POST',headers:{Authorization:`Bearer ${token}`},body:JSON.stringify({version:1,lobby:'m123',self:b,roster:[a,b],nonce:b,action:'check',unusableMs:30000})})).status).toBe(400);
});
it('real hibernating sockets withhold atomic positives until the fixed cohort catches reconcile',async()=>{
 const ta=await auth(a),tb=await auth(b);await routing(a,ta,0);await routing(b,tb,0);const route=await routing(a,ta);
 const x=await socket(a,ta,route.generation),y=await socket(b,tb,route.generation);await flush();
 let seq=0;const send=(ws:WebSocket,body:any)=>ws.send(JSON.stringify({sequence:++seq,kind:'F',payload:JSON.stringify({version:1,...body})}));
 send(x.ws,{action:'begin'});send(x.ws,{action:'done'});x.ws.send(JSON.stringify({sequence:++seq,kind:'S',payload:'1,0,0,0'}));await flush();expect(x.frames.some(f=>f.type==='sparklings')).toBe(false);
 const ns=(env as unknown as Env).COORDINATOR,stub=ns.get(ns.idFromName('coordinator-v1'));
 await evictDurableObject(stub);send(y.ws,{action:'begin'});send(y.ws,{action:'facts',reporter:a,nests:0,facts:Array(38).fill(0),caught:[1,0]});send(y.ws,{action:'done'});await flush();
 // Hibernation wake-up and WebSocket delivery are asynchronous; 30ms is not a completion barrier.
 await expect.poll(()=>x.frames.find(f=>f.type==='sparklings'),{timeout:2000,interval:10}).toBeDefined();
 const atom=x.frames.find(f=>f.type==='sparklings');expect(atom.payload.split(',').map(Number)[2]).toBe(1);
 x.ws.close();y.ws.close();
});
it('malformed and excessive messages close sockets instead of publishing',async()=>{
 const token=await auth(a),r=await routing(a,token),x=await socket(a,token,r.generation);let closed=false;x.ws.addEventListener('close',()=>closed=true);
 x.ws.send(JSON.stringify({sequence:'1',kind:'N',payload:'1'}));await flush();expect(closed).toBe(true);
});
it('replacement socket inherits room caught negatives when the prior witness leaves',async()=>{
 const ta=await auth(a),tb=await auth(b);await routing(a,ta,0);await routing(b,tb,0);const r=await routing(a,ta);
 const x=await socket(a,ta,r.generation);x.ws.send(JSON.stringify({sequence:1,kind:'F',payload:'{"version":1,"action":"begin"}'}));x.ws.send(JSON.stringify({sequence:2,kind:'F',payload:'{"version":1,"action":"done"}'}));x.ws.send(JSON.stringify({sequence:3,kind:'S',payload:'1,0,1,0'}));await flush();
 const y=await socket(b,tb,r.generation);await flush();
 const ns=(env as unknown as Env).COORDINATOR,stub=ns.get(ns.idFromName('coordinator-v1'));
 const caught=await runInDurableObject(stub,(_instance:any,state)=>state.getWebSockets().map(ws=>ws.deserializeAttachment()).find((s:any)=>s.self===b).union[2]);expect(caught).toBe(1);
 x.ws.close();y.ws.close();
});
it('a delayed old nonce leave does not retire the replacement allocator member',async()=>{
 const token=await auth(a),r=await routing(a,token);const x=await socket(a,token,r.generation);
 x.ws.send(JSON.stringify({sequence:1,kind:'F',payload:'{"version":1,"action":"begin"}'}));x.ws.send(JSON.stringify({sequence:2,kind:'F',payload:'{"version":1,"action":"done"}'}));x.ws.send(JSON.stringify({sequence:3,kind:'H',payload:JSON.stringify({version:1,action:'hello',display:true,ticketed:true,leader:true,present:[a],locked:{}})}));await flush();
 const response=await SELF.fetch('https://backup.test/v1/route',{method:'POST',headers:{Authorization:`Bearer ${token}`},body:JSON.stringify({version:1,lobby:'m123',self:a,roster:[a,b],nonce:'oldnonceoldnonce1',action:'leave',unusableMs:0})});expect(response.status).toBe(200);
 const ns=(env as unknown as Env).COORDINATOR,stub=ns.get(ns.idFromName('coordinator-v1'));
 const left=await runInDurableObject(stub,(_instance:any,state)=>JSON.parse(state.storage.sql.exec<{allocation:string}>('SELECT allocation FROM rooms WHERE id=?',r.generation).one().allocation).members[a].left);expect(left).toBe(false);x.ws.close();
});
it('roster expansion retains caught negatives and requires incumbents to reconcile again',async()=>{
 const c='3'.repeat(32),ta=await auth(a),tb=await auth(b);await routing(a,ta,0);await routing(b,tb,0);const r=await routing(a,ta);
 const x=await socket(a,ta,r.generation),y=await socket(b,tb,r.generation);
 for(const s of [x,y]){s.ws.send(JSON.stringify({sequence:1,kind:'F',payload:'{"version":1,"action":"begin"}'}));s.ws.send(JSON.stringify({sequence:2,kind:'F',payload:'{"version":1,"action":"done"}'}));}
 x.ws.send(JSON.stringify({sequence:3,kind:'S',payload:'1,0,1,0'}));await flush();
 const expanded=await SELF.fetch('https://backup.test/v1/route',{method:'POST',headers:{Authorization:`Bearer ${tb}`},body:JSON.stringify({version:1,lobby:'m123',self:b,roster:[a,b,c],nonce:b,action:'check',unusableMs:0,confirmedExpansion:true})});expect(expanded.status).toBe(200);
 const next=await socket(b,tb,r.generation,[a,b,c]);next.ws.send(JSON.stringify({sequence:4,kind:'F',payload:'{"version":1,"action":"begin"}'}));next.ws.send(JSON.stringify({sequence:5,kind:'F',payload:'{"version":1,"action":"done"}'}));next.ws.send(JSON.stringify({sequence:6,kind:'S',payload:'1,0,0,0'}));await flush();
 expect(next.frames.some(f=>f.type==='sparklings')).toBe(false);
 // Even a positive departure by the old witness must not discard an already retained catch.
 const left=await SELF.fetch('https://backup.test/v1/route',{method:'POST',headers:{Authorization:`Bearer ${ta}`},body:JSON.stringify({version:1,lobby:'m123',self:a,roster:[a,b,c],nonce:a,action:'leave',unusableMs:0})});expect(left.status).toBe(200);await flush();
 const atoms=next.frames.filter(f=>f.type==='sparklings');expect(atoms.length).toBeGreaterThan(0);expect(atoms.every(f=>f.payload.split(',')[2]==='1')).toBe(true);
 x.ws.close();y.ws.close();next.ws.close();
});
it('real sockets reject roster isolation and release no missing-cohort automatic state after lease expiry',async()=>{
 const ta=await auth(a),tb=await auth(b);await routing(a,ta,0);await routing(b,tb,0);const r=await routing(a,ta);
 const wrong=await SELF.fetch('https://backup.test/v1/sync',{headers:{Upgrade:'websocket',Authorization:`Bearer ${ta}`,'X-SafariUtils-Lobby':'m124','X-SafariUtils-Player':a,'X-SafariUtils-Roster':[a,b].join(','),'X-SM-Generation':r.generation,'X-SM-Visit':a}});expect(wrong.status).toBe(400);
 const x=await socket(a,ta,r.generation);x.ws.send(JSON.stringify({sequence:1,kind:'F',payload:'{"version":1,"action":"begin"}'}));x.ws.send(JSON.stringify({sequence:2,kind:'F',payload:'{"version":1,"action":"done"}'}));await flush();
 const ns=(env as unknown as Env).COORDINATOR,stub=ns.get(ns.idFromName('coordinator-v1'));
 await runInDurableObject(stub,(_instance:any,state)=>{
  const row=state.storage.sql.exec<{routing:string}>('SELECT routing FROM rooms WHERE id=?',r.generation).one();const routing=JSON.parse(row.routing);routing.members[b].renewed=0;
  state.storage.sql.exec('UPDATE rooms SET routing=? WHERE id=?',JSON.stringify(routing),r.generation);
 });
 x.ws.send(JSON.stringify({sequence:3,kind:'S',payload:'1,0,0,0'}));await flush();expect(x.frames.some(f=>f.type==='sparklings')).toBe(false);x.ws.close();
});

it('protocol 2 reconciles the backup cohort, scopes replay and replaces completion after reconnect',async()=>{
 const ta=await auth(a),tb=await auth(b);
 await routing(a,ta,0);await routing(b,tb,0);
 const {generation}=await routing(a,ta);
 const x=await socket(a,ta,generation,[a,b],2),y=await socket(b,tb,generation,[a,b],2);
 const va=crypto.randomUUID(),vb=crypto.randomUUID();
 function sender(peer:typeof x){
  let sequence=0;
  return (kind:string,payload:object|string)=>peer.ws.send(JSON.stringify({
   sequence:++sequence,kind,payload:typeof payload==='string'?payload:JSON.stringify({version:1,...payload})
  }));
 }
 const sx=sender(x),sy=sender(y);
 const bootstrap=(send:ReturnType<typeof sender>)=>{send('F',{action:'begin'});send('F',{action:'done'});};
 const hello=(send:ReturnType<typeof sender>,visitId:string,resumeRunId:string|null)=>send('H',{
  action:'hello',visitId,resumeRunId,capabilities:['run-reconciliation-v1','critter-completion-v1'],
  ticketed:true,display:true,leader:false,present:[a,b],locked:{}
 });
 const until=(fn:()=>unknown)=>expect.poll(fn,{timeout:3000,interval:10}).toBeTruthy();
 bootstrap(sx);hello(sx,va,null);
 await until(()=>x.frames.some(f=>f.type==='reconcile'&&f.status==='pending'));
 expect(x.frames.map(f=>f.type)).toEqual(['welcome','reconcile']);
 bootstrap(sy);hello(sy,vb,null);
 await until(()=>y.frames.some(f=>f.type==='replay-complete'));
 hello(sx,va,null);
 await until(()=>x.frames.some(f=>f.type==='replay-complete'));
 const runId=x.frames.find(f=>f.type==='replay-complete').runId;
 const rows=Array.from({length:37},()=>[0,0,0]);rows[6]=[7,3,3];
 const usage=[1,1,1,1,2,1];
 sx('U',usage.join(','));
 sx('C',{runId,visitId:va,rows});
 await until(()=>y.frames.some(f=>f.kind==='C'));
 const z=await socket(b,tb,generation,[a,b],2),sz=sender(z);
 bootstrap(sz);hello(sz,vb,runId);
 await until(()=>z.frames.some(f=>f.type==='replay-complete'));
 const replay=z.frames.findIndex(f=>f.kind==='C');
 expect(replay).toBeGreaterThan(z.frames.findIndex(f=>f.type==='reconcile'));
 expect(replay).toBeLessThan(z.frames.findIndex(f=>f.type==='replay-complete'));
 expect(z.frames[replay]).toMatchObject({runId,sender:a});
 expect(z.frames.find(f=>f.kind==='U')).toMatchObject({runId,sender:a,inventoryFresh:false});
 expect(z.frames.find(f=>f.type==='reconcile').status).toBe('resumed');
 hello(sx,va,runId);
 await until(()=>x.frames.filter(f=>f.type==='replay-complete').length===2);
 rows[6]=[0,0,0];sx('C',{runId,visitId:va,rows});
 await until(()=>z.frames.filter(f=>f.kind==='C').length===2);
 expect(JSON.parse(z.frames.filter(f=>f.kind==='C').at(-1).payload).rows[6]).toEqual([0,0,0]);
 x.ws.close();z.ws.close();
});

it('transport 3 batches ordinary relays, refreshes the full revision, and renews without replacing the socket',async()=>{
 const ta=await auth(a),tb=await auth(b);
 await routing(a,ta,0);await routing(b,tb,0);
 const {generation}=await routing(a,ta);
 const x=await socket(a,ta,generation,[a,b],3),y=await socket(b,tb,generation,[a,b],2);
 const va=crypto.randomUUID(),vb=crypto.randomUUID();
 const frame=(sequence:number,kind:string,payload:object|string)=>({
  sequence,kind,payload:typeof payload==='string'?payload:JSON.stringify({version:1,...payload})
 });
 const send=(ws:WebSocket,message:object)=>ws.send(JSON.stringify(message));
 const hello=(visitId:string,sequence=3)=>frame(sequence,'H',{action:'hello',visitId,resumeRunId:null,
  capabilities:['run-reconciliation-v1','critter-completion-v1'],ticketed:true,display:true,leader:false,present:[a,b],locked:{}});
 const until=(fn:()=>unknown)=>expect.poll(fn,{timeout:3000,interval:10}).toBeTruthy();
 for(const ws of [x.ws,y.ws]){
  send(ws,frame(1,'F',{action:'begin'}));send(ws,frame(2,'F',{action:'done'}));
 }
 send(x.ws,hello(va));send(y.ws,hello(vb));
 await until(()=>x.frames.some(f=>f.type==='reconcile')&&y.frames.some(f=>f.type==='reconcile'));
 if(!x.frames.some(f=>f.type==='replay-complete'))send(x.ws,hello(va,4));
 if(!y.frames.some(f=>f.type==='replay-complete'))send(y.ws,hello(vb,4));
 await until(()=>x.frames.some(f=>f.type==='replay-complete')&&y.frames.some(f=>f.type==='replay-complete'));
 expect(x.frames[0].features).toContain('efficient-transport-v1');
 expect(y.frames[0].features).not.toContain('efficient-transport-v1');
 const runId=x.frames.find(f=>f.type==='replay-complete').runId;
 const rows=Array.from({length:37},()=>[1,2,3]);
 send(x.ws,{type:'batch',frames:[frame(5,'N','1'),frame(6,'C',{runId,visitId:va,rows})]});
 await until(()=>y.frames.some(f=>f.kind==='N')&&y.frames.some(f=>f.kind==='C'));
 send(x.ws,{type:'batch',frames:[frame(7,'F',{runId,visitId:va,revision:6})]});
 await until(()=>y.frames.some(f=>f.kind==='C'&&f.sequence===7));
 expect(JSON.parse(y.frames.find(f=>f.kind==='C'&&f.sequence===7).payload).rows).toEqual(rows);
 send(x.ws,frame(8,'F',{runId,visitId:va,revision:7}));
 await until(()=>x.frames.some(f=>f.type==='completion-resync'));
 expect(x.frames.find(f=>f.type==='completion-resync')).toMatchObject({runId,visitId:va});
 expect(x.frames.filter(f=>f.type==='replay-complete')).toHaveLength(1);
 send(x.ws,{type:'renew',token:ta});
 await until(()=>x.frames.some(f=>f.type==='renewed'));
 expect(x.frames.find(f=>f.type==='renewed').expiresInMillis).toBeLessThanOrEqual(600000);
 x.ws.close();y.ws.close();
});
