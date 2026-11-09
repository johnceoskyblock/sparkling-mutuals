// Manual, authenticated TLS smoke. Never logs tokens, raw frames or account details.
import assert from 'node:assert/strict';
import {randomBytes} from 'node:crypto';
import WebSocket from 'ws';
const origin=new URL(process.argv[2]);assert.equal(origin.protocol,'https:');
const ids=[randomBytes(16).toString('hex'),randomBytes(16).toString('hex')].sort();
const lobby='m'+Date.now(),nonces=ids.map(()=>randomBytes(16).toString('hex'));
const tokens=[],connections=[],checks=[];
async function post(path,body,token){return fetch(new URL(path,origin),{method:'POST',headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:JSON.stringify(body)});}
async function route(i,unusableMs,action='check'){const r=await post('/v1/route',{version:1,lobby,self:ids[i],roster:ids,nonce:nonces[i],action,unusableMs},tokens[i]);assert.equal(r.status,200);return r.json();}
async function wait(condition,ms=8000){const end=Date.now()+ms;while(!condition()){assert.ok(Date.now()<end,'Timed out waiting for expected protocol state');await new Promise(r=>setTimeout(r,25));}}
async function connect(i,generation){
 const url=new URL('/v1/sync',origin);url.protocol='wss:';
 const ws=new WebSocket(url,{headers:{Authorization:'Bearer '+tokens[i],'X-SafariUtils-Lobby':lobby,'X-SafariUtils-Player':ids[i],'X-SafariUtils-Roster':ids.join(','),'X-SM-Generation':generation,'X-SM-Visit':nonces[i]}});
 const frames=[];ws.on('message',data=>frames.push(JSON.parse(data.toString())));ws.on('error',()=>{});
 await new Promise((resolve,reject)=>{ws.once('open',resolve);ws.once('error',()=>reject(new Error('TLS WebSocket upgrade failed')));});
 let sequence=0;const send=(kind,payload)=>ws.send(JSON.stringify({sequence:++sequence,kind,payload:typeof payload==='string'?payload:JSON.stringify(payload)}));
 const c={ws,frames,send,bootstrap:(action,fields={})=>send('F',{version:1,action,...fields})};connections.push(c);await wait(()=>frames.some(f=>f.type==='welcome'));return c;
}
try{
 assert.equal((await post('/v1/route',{},'invalid')).status,401);checks.push('invalid bearer rejected');
 for(const id of ids){const r=await post('/v1/auth/uuid/session',{uuid:id});assert.equal(r.status,200);tokens.push((await r.json()).token);}
 assert.equal((await post('/v1/route',{version:1,lobby,self:ids[1],roster:ids,nonce:nonces[1],action:'check',unusableMs:0},tokens[0])).status,400);checks.push('foreign identity rejected');
 const p=await route(0,0);assert.equal(p.provider,'primary');await route(1,0);
 assert.equal((await route(0,29999)).provider,'primary');const selected=await route(0,30000);assert.equal(selected.provider,'backup');assert.equal((await route(1,0)).generation,selected.generation);checks.push('sustained failure and shared sticky route');
 const x=await connect(0,selected.generation),y=await connect(1,selected.generation);
 x.bootstrap('begin');x.bootstrap('done');x.send('S','1,0,0,0');await new Promise(r=>setTimeout(r,250));assert.ok(!x.frames.some(f=>f.type==='sparklings'));checks.push('cohort barrier before atomic positives');
 y.bootstrap('begin');y.bootstrap('facts',{reporter:ids[1],source:'reporter',nests:0,facts:Array(38).fill(0),caught:[1,0]});y.bootstrap('done');
 await wait(()=>x.frames.some(f=>f.type==='sparklings'));
 const atomic=x.frames.filter(f=>f.type==='sparklings');for(const f of atomic){const v=f.payload.split(',').map(Number);assert.equal(v[0]&~v[2],0);}checks.push('caught-before-found atomic suppression');
 x.send('H',{version:1,action:'hello',display:true,ticketed:true,leader:true,present:ids,locked:{}});
 y.send('H',{version:1,action:'hello',display:true,ticketed:true,leader:false,present:ids,locked:{}});
 x.send('H',{version:1,action:'report',player:ids[0],biome:'FOREST'});y.send('H',{version:1,action:'report',player:ids[1],biome:'FOREST'});
 await wait(()=>x.frames.some(f=>f.type==='hotspots'&&f.payload.initialDone));
 const roles=x.frames.filter(f=>f.type==='hotspots'&&f.payload.initialDone).at(-1).payload.assignments;assert.equal(Object.keys(roles).length,2);assert.equal(new Set(Object.values(roles)).size,2);checks.push('timed allocator returns distinct immutable roles');
 console.log(JSON.stringify({origin:origin.origin,passed:true,checks,liveHypixelTested:false}));
}finally{
 for(let i=0;i<tokens.length;i++){try{await route(i,0,'leave');}catch{}}
 for(const c of connections){c.ws.close();c.ws.terminate();}
}
