import {DurableObject} from 'cloudflare:workers';
import {issueToken,verifyToken} from './auth';
import {Allocation} from './allocator';
import {RouteRoom,request as routeRequest,route} from './routing';
import {Attachment,Room,attachment} from './room';
import {check,identity,object,text,uuid} from './protocol';
export interface Env {COORDINATOR:DurableObjectNamespace<Coordinator>;SIGNING_SECRET:string;BACKUP_ENABLED:string}
const json=(v:unknown,status=200)=>Response.json(v,{status,headers:{'Cache-Control':'no-store'}});
export default {fetch(request:Request,env:Env){return env.COORDINATOR.get(env.COORDINATOR.idFromName('coordinator-v1')).fetch(request);}};
export class Coordinator extends DurableObject<Env> {
 constructor(ctx:DurableObjectState,env:Env){super(ctx,env);
  ctx.storage.sql.exec('CREATE TABLE IF NOT EXISTS rooms (id TEXT PRIMARY KEY, lobby TEXT NOT NULL, renewed INTEGER NOT NULL, routing TEXT NOT NULL, allocation TEXT NOT NULL)');
  ctx.storage.sql.exec('CREATE INDEX IF NOT EXISTS rooms_lobby ON rooms(lobby)');
  ctx.storage.sql.exec('CREATE TABLE IF NOT EXISTS budgets (id TEXT PRIMARY KEY, expires INTEGER NOT NULL, count INTEGER NOT NULL)');
  ctx.storage.sql.exec('CREATE TABLE IF NOT EXISTS backup_fairness (roster TEXT PRIMARY KEY, updated INTEGER NOT NULL, counts TEXT NOT NULL)');
 }
 private budget(id:string,limit:number,window:number,now:number){
  const expires=Math.floor(now/window)*window+window,key=id+':'+expires;
  const old=this.ctx.storage.sql.exec<{count:number}>('SELECT count FROM budgets WHERE id=?',key).toArray()[0]?.count??0;
  if(old>=limit)return false;
  this.ctx.storage.sql.exec('DELETE FROM budgets WHERE expires<=?',now);
  this.ctx.storage.sql.exec('INSERT INTO budgets(id,expires,count) VALUES(?,?,?) ON CONFLICT(id) DO UPDATE SET count=excluded.count',key,expires,old+1);return true;
 }
 private load(id:string){const row=this.ctx.storage.sql.exec<{routing:string;allocation:string}>('SELECT routing,allocation FROM rooms WHERE id=?',id).toArray()[0];return row?{routing:JSON.parse(row.routing) as RouteRoom,allocation:Allocation.restore(JSON.parse(row.allocation))}:null;}
 private save(r:RouteRoom,a:Allocation){this.ctx.storage.sql.exec('INSERT INTO rooms(id,lobby,renewed,routing,allocation) VALUES(?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET renewed=excluded.renewed,routing=excluded.routing,allocation=excluded.allocation',r.generation,r.lobby,r.renewed,JSON.stringify(r),JSON.stringify(a));
  const group=r.roster.join(','),counts=JSON.stringify(a.backupWins),old=this.ctx.storage.sql.exec<{counts:string}>('SELECT counts FROM backup_fairness WHERE roster=?',group).toArray()[0];
  if(Object.keys(a.backupWins).length&&old?.counts!==counts){this.ctx.storage.sql.exec('DELETE FROM backup_fairness WHERE updated<?',Date.now()-30*86400000);this.ctx.storage.sql.exec('INSERT INTO backup_fairness(roster,updated,counts) VALUES(?,?,?) ON CONFLICT(roster) DO UPDATE SET updated=excluded.updated,counts=excluded.counts',group,Date.now(),counts);this.ctx.storage.sql.exec('DELETE FROM backup_fairness WHERE roster IN (SELECT roster FROM backup_fairness ORDER BY updated DESC LIMIT -1 OFFSET 256)');}
 }
 private sockets(room:string){return this.ctx.getWebSockets().filter(ws=>{const s=ws.deserializeAttachment() as Attachment;return s?.room===room&&s.expires>Date.now();});}
 private room(id:string){const row=this.load(id);if(!row)return null;const sessions=this.sockets(id).map(ws=>ws.deserializeAttachment() as Attachment);return new Room(row.routing,row.allocation,sessions);}
 private persist(room:Room){const old=this.load(room.route.generation);if(!old||JSON.stringify(old.routing)!==JSON.stringify(room.route)||JSON.stringify(old.allocation)!==JSON.stringify(room.allocation))this.save(room.route,room.allocation);for(const ws of this.sockets(room.route.generation)){const old=ws.deserializeAttachment() as Attachment;const next=room.sessions.find(s=>s.self===old.self&&s.nonce===old.nonce);if(next)ws.serializeAttachment(next);}}
 private deliver(room:Room,frames:unknown[]){for(const ws of this.sockets(room.route.generation)){const s=ws.deserializeAttachment() as Attachment;if(room.route.members[s.self]?.retired)continue;for(const f of frames){const type=(f as {type:string}).type;if(s.bootstrap.stage!=='done'&&(type==='sparklings'||type==='state'&&(f as {kind:string}).kind==='S'))continue;ws.send(JSON.stringify(f));}ws.send(JSON.stringify({type:'hotspots',payload:room.allocation.snapshot(Date.now(),s.self,room.ready&&s.bootstrap.stage==='done')}));}}
 private async schedule(){
  const now=Date.now();let next=Infinity;
  for(const ws of this.ctx.getWebSockets()){const s=ws.deserializeAttachment() as Attachment;if(s.expires<=now)continue;next=Math.min(next,s.expires);const r=this.room(s.room);if(r?.ready&&r.allocation.due!==null)next=Math.min(next,Math.max(now+1,r.allocation.due));
   for(const at of Object.values(r?.allocation.assignedAt??{}))if(at+2000>now)next=Math.min(next,at+2000);}
  if(Number.isFinite(next))await this.ctx.storage.setAlarm(next);else await this.ctx.storage.deleteAlarm();
 }
 async fetch(req:Request):Promise<Response>{
  const now=Date.now(),url=new URL(req.url);
  try{
   if(this.env.BACKUP_ENABLED!=='true')return json({error:'disabled'},503);
   if(!this.env.SIGNING_SECRET||this.env.SIGNING_SECRET.length<32)return json({error:'not configured'},503);
   // Hash network admission keys, never retain raw addresses or log request bodies/headers.
   const ip=req.headers.get('CF-Connecting-IP')??'local';const digest=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(this.env.SIGNING_SECRET+':'+ip));
   const ipKey=Array.from(new Uint8Array(digest)).map(v=>v.toString(16).padStart(2,'0')).join('');
   if(!this.budget('global',24000,86400000,now)||!this.budget('ip:'+ipKey,6000,86400000,now))return json({error:'budget'},429);
   if(url.pathname==='/v1/auth/uuid/session'&&req.method==='POST'){
    const raw=await req.text();check(raw,raw.length<=128);const id=uuid(object(JSON.parse(raw)).uuid);
    if(!this.budget('auth:'+id,60,3600000,now))return json({error:'budget'},429);
    return json({token:await issueToken(id,this.env.SIGNING_SECRET,url.origin,now),expiresInSeconds:600});
   }
   let self:string;try{self=await verifyToken((req.headers.get('Authorization')??'').replace(/^Bearer /,''),this.env.SIGNING_SECRET,url.origin,now);}catch{return json({error:'unauthorized'},401);}
   if(!this.budget('account:'+self,120,60000,now))return json({error:'budget'},429);
   if(url.pathname==='/v1/route'&&req.method==='POST'){
    const raw=await req.text();check(raw,raw.length<=1024);const q=routeRequest(JSON.parse(raw));check(q,q.self===self);
    const rows=this.ctx.storage.sql.exec<{id:string;routing:string;allocation:string}>('SELECT id,routing,allocation FROM rooms WHERE lobby=?',q.lobby).toArray();
    const overlaps=rows.map(row=>({row,r:JSON.parse(row.routing) as RouteRoom})).filter(x=>x.r.roster.some(id=>q.roster.includes(id))&&now-x.r.renewed<600000);
    check(q,overlaps.length<=1);const old=overlaps[0];const result=route(old?.r,q,now);
    const priorWins=this.ctx.storage.sql.exec<{counts:string}>('SELECT counts FROM backup_fairness WHERE roster=? AND updated>?',result.room.roster.join(','),now-30*86400000).toArray()[0];
    let a=old&&old.r.generation===result.generation?Allocation.restore(JSON.parse(old.row.allocation)):new Allocation(result.room.roster,priorWins?JSON.parse(priorWins.counts):{});
    if(old&&old.r.generation!==result.generation)this.ctx.storage.sql.exec('DELETE FROM rooms WHERE id=?',old.r.generation);
    if(a.roster.join()!==result.room.roster.join()){a.roster=result.room.roster;a.fairness={};a.backupWins={};a.seeded=null;}
    this.ctx.storage.sql.exec('DELETE FROM rooms WHERE renewed<=?',now-600000);
    check(q,this.ctx.storage.sql.exec<{n:number}>('SELECT count(*) AS n FROM rooms').one().n<128||!!old);
    this.save(result.room,a);
    if(q.action==='leave'){a.leave(self);this.save(result.room,a);const r=this.room(result.generation);if(r)this.deliver(r,r.ready?[{type:'sparklings',payload:r.union().join(',')}]:[]);}
    return json({version:1,generation:result.generation,provider:result.provider,admitted:result.admitted,nonce:q.nonce});
   }
   if(url.pathname==='/v1/sync'&&req.headers.get('Upgrade')?.toLowerCase()==='websocket'){
    const i=identity(req.headers.get('X-SafariUtils-Lobby'),req.headers.get('X-SafariUtils-Player'),req.headers.get('X-SafariUtils-Roster')?.split(','));check(i,i.self===self);
    const gen=text(req.headers.get('X-SM-Generation'),64),nonce=text(req.headers.get('X-SM-Visit'),64),row=this.load(gen),m=row?.routing.members[self];
    check(row,!!row&&row.routing.provider==='backup'&&row.routing.lobby===i.lobby&&row.routing.roster.join()===i.roster.join()&&!!m&&!m.retired&&m.nonce===nonce&&now-row.routing.renewed<600000);
    check(row,this.ctx.getWebSockets().length<40);
    // Only one active backup connection per current registration.
    for(const old of this.sockets(gen)){const prior=old.deserializeAttachment() as Attachment;if(prior.self===self){prior.expires=0;old.serializeAttachment(prior);old.close(1000,'replaced');}}
    const pair=new WebSocketPair(),client=pair[0],server=pair[1];this.ctx.acceptWebSocket(server);
    const s=attachment(gen,self,nonce,i.roster,now);server.serializeAttachment(s);
    server.send(JSON.stringify({type:'welcome',self,roster:i.roster,features:['hotspots-v1','biome-reports-v1','room-sparklings-v1','failover-bootstrap-v1']}));
    const room=this.room(gen)!;for(const frame of room.replay(s))server.send(JSON.stringify(frame));
    await this.schedule();return new Response(null,{status:101,webSocket:client});
   }
   return json({error:'not found'},404);
  }catch{return json({error:'invalid request'},400);}
 }
 async webSocketMessage(ws:WebSocket,message:string|ArrayBuffer){
  try{
   check(message,typeof message==='string');const s=ws.deserializeAttachment() as Attachment,room=this.room(s.room);check(room,!!room);
   const member=room!.route.members[s.self];check(s,!!member&&!member.retired&&member.nonce===s.nonce&&room!.route.roster.join()===s.roster.join());
   const current=room!.sessions.find(x=>x.self===s.self&&x.nonce===s.nonce)!;const result=room!.receive(current,message as string,Date.now());
   this.persist(room!);this.deliver(room!,result.broadcast);if(result.leave)ws.close(1000,'departed');await this.schedule();
  }catch{ws.close(1008,'invalid protocol');}
 }
 async webSocketClose(ws:WebSocket){const s=ws.deserializeAttachment() as Attachment;const r=this.room(s.room);if(r&&!this.sockets(s.room).some(other=>other!==ws&&(other.deserializeAttachment() as Attachment).self===s.self)){r.allocation.disconnect(s.self);this.save(r.route,r.allocation);}await this.schedule();}
 async webSocketError(ws:WebSocket){ws.close(1011,'transport');await this.webSocketClose(ws);}
 async alarm(){const now=Date.now();const visited=new Set<string>();for(const ws of this.ctx.getWebSockets()){
  const s=ws.deserializeAttachment() as Attachment;if(now>=s.expires){ws.close(1000,'renew lease');continue;}if(visited.has(s.room))continue;visited.add(s.room);
  const r=this.room(s.room);if(r){r.allocation.allocate(now,r.ready);this.persist(r);this.deliver(r,[]);}}
  await this.schedule();
 }
}
