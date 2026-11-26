// Hosted fixture-only smoke: never print tokens, identities or wire payloads.
import assert from 'node:assert/strict';
import {randomBytes,randomUUID} from 'node:crypto';
import WebSocket from 'ws';
const origin=new URL(process.argv[2]);assert.equal(origin.protocol,'https:');
const ids=Array.from({length:3},()=>randomBytes(16).toString('hex')).sort(),tokens=[],sockets=[];
const visits=ids.map(()=>randomUUID()),nonce=ids.map(()=>randomBytes(16).toString('hex')),lobby='m'+Date.now();
let roster=ids.slice(0,2),generation;
async function post(path,body,token){const r=await fetch(new URL(path,origin),{method:'POST',headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:JSON.stringify(body)});assert.equal(r.status,200);return r.json();}
async function route(i,action='check'){return post('/v1/route',{version:1,lobby,self:ids[i],roster,nonce:nonce[i],action,unusableMs:30000,confirmedExpansion:true},tokens[i]);}
async function until(fn,ms=8000){const end=Date.now()+ms;while(!fn()){assert.ok(Date.now()<end,'Protocol boundary timeout');await new Promise(r=>setTimeout(r,40));}}
async function connect(i){
 const url=new URL('/v1/sync',origin);url.protocol='wss:';
 const ws=new WebSocket(url,{headers:{Authorization:'Bearer '+tokens[i],'X-SafariUtils-Lobby':lobby,'X-SafariUtils-Player':ids[i],'X-SafariUtils-Roster':roster.join(','),'X-SM-Generation':generation,'X-SM-Visit':nonce[i],'X-SafariUtils-Sync-Protocol':'3'}});
 const c={ws,i,frames:[],seq:0,run:null};sockets.push(c);ws.on('error',()=>{});ws.on('message',v=>c.frames.push(JSON.parse(v.toString())));
 await new Promise((resolve,reject)=>{ws.once('open',resolve);ws.once('error',()=>reject(new Error('Upgrade failed')))});
 await until(()=>c.frames.some(f=>f.type==='welcome'));return c;
}
function frame(c,kind,payload){c.ws.send(JSON.stringify({sequence:++c.seq,kind,payload:typeof payload==='string'?payload:JSON.stringify({version:1,...payload})}));}
function hello(c){frame(c,'H',{action:'hello',visitId:visits[c.i],resumeRunId:c.run,capabilities:['run-reconciliation-v1','critter-completion-v1'],display:true,ticketed:true,leader:c.i===0,present:roster,locked:{},hotspotPreference:['HAUNTED','FOREST','ICY','CAVERN'],startingItems:{birdFood:0,shiningCoins:0}});}
function bootstrap(c){frame(c,'F',{action:'begin'});frame(c,'F',{action:'done'});hello(c);}
async function ready(group){for(let attempt=0;attempt<5;attempt++){
 await new Promise(r=>setTimeout(r,350));
 if(group.every(c=>c.frames.some(f=>f.type==='replay-complete'&&f.connectionId===c.frames.filter(v=>v.type==='welcome').at(-1).connectionId)))return;
 for(const c of group)hello(c);
 }throw Error('Replay did not complete');}
try{
 for(const id of ids)tokens.push((await post('/v1/auth/uuid/session',{uuid:id})).token);
 generation=(await route(0)).generation;await route(1);
 const x=await connect(0),y=await connect(1);bootstrap(x);bootstrap(y);await ready([x,y]);
 x.run=x.frames.find(f=>f.type==='replay-complete').runId;y.run=x.run;
 const welcome=x.frames.find(f=>f.type==='welcome');
 for(const f of ['roster-expand-v1','visit-ended-v1','hotspot-preferences-v1'])assert.ok(welcome.features.includes(f));
 const oldSequence=x.seq;roster=ids;
 x.ws.send(JSON.stringify({type:'expand',connectionId:welcome.connectionId,visitId:visits[0],runId:x.run,roster}));
 await until(()=>x.frames.filter(f=>f.type==='welcome').length===2);
 assert.notEqual(x.frames.filter(f=>f.type==='welcome').at(-1).connectionId,welcome.connectionId);
 assert.equal(x.seq,oldSequence);await route(1);await route(2);
 const newer=await connect(1),guest=await connect(2);newer.run=x.run;guest.run=x.run;
 bootstrap(x);bootstrap(newer);bootstrap(guest);await ready([x,newer,guest]);
 for(const c of [x,newer,guest]){frame(c,'H',{action:'leave',visitId:visits[c.i],runId:c.run,visitEnded:true});}
 await new Promise(r=>setTimeout(r,350));
 console.log(JSON.stringify({passed:true,checks:['TLS v3 negotiation','same-socket roster expansion','new logical connection identity','continued sequence','three-member ordered replay','confirmed visit-end leave'],liveHypixelTested:false}));
}finally{
 for(let i=0;i<tokens.length;i++)try{await route(i,'leave')}catch{}
 for(const c of sockets)c.ws.terminate();
}
