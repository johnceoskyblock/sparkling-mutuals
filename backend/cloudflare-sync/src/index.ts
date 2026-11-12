import {CompletionAttachment,receiveCompletion,completionFrame} from './completion';
import {DurableObject} from 'cloudflare:workers';
import {issueToken,verifyToken} from './auth';
import {Allocation} from './allocator';
import {RouteRoom,request as routeRequest,route} from './routing';
import {Attachment,Room,attachment,mergeEvidence} from './room';
import {check,identity,object,text,uuid} from './protocol';
export interface Env {COORDINATOR:DurableObjectNamespace<Coordinator>;SIGNING_SECRET:string;BACKUP_ENABLED:string;SYNC_MIN_PROTOCOL?:string}
const json=(v:unknown,status=200)=>Response.json(v,{status,headers:{'Cache-Control':'no-store'}});
function transportVersion(request:Request){
 const version=Number(request.headers.get('X-SafariUtils-Sync-Protocol'));
 return version===2||version===3?version:1;
}
export default {fetch(request:Request,env:Env){
 if(new URL(request.url).pathname==='/v1/sync'&&request.headers.get('Upgrade')?.toLowerCase()==='websocket'&&
    transportVersion(request)<Number(env.SYNC_MIN_PROTOCOL??1))
  return Response.json({error:'sync_update_required'},{status:426,headers:{'x-safari-sync-update-required':'true','Cache-Control':'no-store'}});
 return env.COORDINATOR.get(env.COORDINATOR.idFromName('coordinator-v1')).fetch(request);
}};
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
 private sockets(room:string){return this.ctx.getWebSockets().filter(ws=>{const s=ws.deserializeAttachment() as Attachment;return (s as any)?.channel!=='completion'&&s?.room===room&&s.expires>Date.now();});}
 private room(id:string){const row=this.load(id);if(!row)return null;const sessions=this.sockets(id).map(ws=>ws.deserializeAttachment() as Attachment).filter(s=>s.roster.join()===row.routing.roster.join());return new Room(row.routing,row.allocation,sessions);}
 private persist(room:Room){const old=this.load(room.route.generation);if(!old||JSON.stringify(old.routing)!==JSON.stringify(room.route)||JSON.stringify(old.allocation)!==JSON.stringify(room.allocation))this.save(room.route,room.allocation);for(const ws of this.sockets(room.route.generation)){const old=ws.deserializeAttachment() as Attachment;const next=room.sessions.find(s=>s.self===old.self&&s.nonce===old.nonce);if(next)ws.serializeAttachment(next);}}
 private scoped(room:Room,frame:unknown){
  return {...frame as object,runId:room.allocation.run};
 }
 private hotspots(room:Room,s:Attachment,now:number){
  const snapshot=room.allocation.snapshot(now,s.self,room.ready&&s.bootstrap.stage==='done');
  return {type:'hotspots',payload:s.protocol?.efficient?{...snapshot,reports:room.allocation.reports}:snapshot};
 }
 private deliver(room:Room,frames:unknown[]){
  const now=Date.now();
  for(const ws of this.sockets(room.route.generation)){
   const s=ws.deserializeAttachment() as Attachment;
   if(s.roster.join()!==room.route.roster.join()){ws.close(1008,'roster changed');continue;}
   if(room.route.members[s.self]?.retired)continue;
   if(s.protocol){
    if(!room.replayReady&&s.protocol.ready){
     s.protocol.ready=false;
     ws.serializeAttachment(s);
     ws.send(JSON.stringify(this.scoped(room,{type:'reconciling'})));
    }
    if(!s.protocol.ready)continue;
   }
   for(const f of frames){
    const {type,kind}=f as {type:string;kind?:string};
    if(kind==='C'&&!s.protocol?.completion)continue;
    if(s.bootstrap.stage!=='done'&&(type==='sparklings'||kind==='S'))continue;
    ws.send(JSON.stringify(s.protocol?this.scoped(room,f):f));
   }
   ws.send(JSON.stringify(this.hotspots(room,s,now)));
  }
 }
 private deliverReconciliation(ws:WebSocket,room:Room,s:Attachment,frames:unknown[],now:number){
  for(const frame of frames)ws.send(JSON.stringify(frame));
  if(!s.protocol?.ready||!frames.some(frame=>(frame as {type:string}).type==='reconcile'))return;
  ws.send(JSON.stringify(this.hotspots(room,s,now)));
  for(const frame of room.replay(s,now))ws.send(JSON.stringify(this.scoped(room,frame)));
  ws.send(JSON.stringify({type:'replay-complete',version:1,connectionId:s.protocol.connectionId,
   visitId:s.protocol.visitId,runId:room.allocation.run}));
 }
 private completionSockets(room:string){return this.ctx.getWebSockets().filter(ws=>{const s=ws.deserializeAttachment() as CompletionAttachment;return s?.channel==='completion'&&s.room===room&&s.expires>Date.now();});}
 private validCompletion(s:CompletionAttachment,now:number){const r=this.load(s.room)?.routing,m=r?.members[s.self];return !!r&&!!m&&!m.retired&&m.nonce===s.nonce&&r.roster.join()===s.roster.join()&&now-r.renewed<600000&&now<s.expires;}
 private async schedule(){
  const now=Date.now();let next=Infinity;
  for(const ws of this.ctx.getWebSockets()){const s=ws.deserializeAttachment() as Attachment;if(s.expires<=now)continue;next=Math.min(next,s.expires);if((s as any).channel==='completion')continue;const r=this.room(s.room);if(r?.ready&&r.allocation.due!==null)next=Math.min(next,Math.max(now+1,r.allocation.due));
   for(const at of Object.values(r?.allocation.assignedAt??{}))if(at+2000>now)next=Math.min(next,at+2000);}
  if(Number.isFinite(next))await this.ctx.storage.setAlarm(next);else await this.ctx.storage.deleteAlarm();
 }
 async fetch(req:Request):Promise<Response>{
  const now=Date.now(),url=new URL(req.url);
  try{
   const transport=transportVersion(req);
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
    check(q,overlaps.length<=1);const old=overlaps[0],previousRoster=old?.r.roster.join();const result=route(old?.r,q,now);
    if(old&&previousRoster!==result.room.roster.join()&&old.r.generation===result.generation){
     // A bounded routing-epoch bridge, not inventory/history: survive closing old-roster sockets.
     const carry=result.room.carry??{caught:[0,0],evidence:[]};
     for(const ws of this.sockets(result.generation)){const s=ws.deserializeAttachment() as Attachment;for(let i=0;i<2;i++)carry.caught[i]|=s.union[i+2];for(const e of s.evidence)mergeEvidence(carry.evidence,e);}
     result.room.carry=carry;
    }
    const priorWins=this.ctx.storage.sql.exec<{counts:string}>('SELECT counts FROM backup_fairness WHERE roster=? AND updated>?',result.room.roster.join(','),now-30*86400000).toArray()[0];
    let a=old&&old.r.generation===result.generation?Allocation.restore(JSON.parse(old.row.allocation)):new Allocation(result.room.roster,priorWins?JSON.parse(priorWins.counts):{});
    if(old&&old.r.generation!==result.generation)this.ctx.storage.sql.exec('DELETE FROM rooms WHERE id=?',old.r.generation);
    if(a.roster.join()!==result.room.roster.join()){a.roster=result.room.roster;a.fairness={};a.backupWins={};a.seeded=null;}
    this.ctx.storage.sql.exec('DELETE FROM rooms WHERE renewed<=?',now-600000);
    check(q,this.ctx.storage.sql.exec<{n:number}>('SELECT count(*) AS n FROM rooms').one().n<128||!!old);
    this.save(result.room,a);
    if(q.action==='leave'&&result.room.members[self]?.nonce===q.nonce&&result.room.members[self].retired){a.leave(self);this.save(result.room,a);const r=this.room(result.generation);if(r)this.deliver(r,r.ready?[{type:'sparklings',payload:r.union().join(',')}]:[]);}
    if(q.action==='check'&&result.provider==='backup'){const r=this.room(result.generation);if(r&&!r.ready)this.deliver(r,[{type:'reconciling'}]);}
    return json({version:1,generation:result.generation,provider:result.provider,admitted:result.admitted,nonce:q.nonce});
   }
   if(url.pathname==='/v1/completion-sync'&&req.headers.get('Upgrade')?.toLowerCase()==='websocket'){
    const i=identity(req.headers.get('X-SafariUtils-Lobby'),req.headers.get('X-SafariUtils-Player'),req.headers.get('X-SafariUtils-Roster')?.split(','));check(i,i.self===self);
    const gen=text(req.headers.get('X-SM-Generation'),64),nonce=text(req.headers.get('X-SM-Visit'),64),row=this.load(gen),m=row?.routing.members[self];
    check(row,!!row&&row.routing.lobby===i.lobby&&row.routing.roster.join()===i.roster.join()&&!!m&&!m.retired&&m.nonce===nonce&&now-row.routing.renewed<600000);
    check(row,this.ctx.getWebSockets().filter(w=>(w.deserializeAttachment() as any)?.channel==='completion').length<40&&this.ctx.getWebSockets().length<80);
    for(const old of this.completionSockets(gen)){const s=old.deserializeAttachment() as CompletionAttachment;if(s.self===self){s.expires=0;old.serializeAttachment(s);old.close(1000,'replaced');}}
    const pair=new WebSocketPair(),server=pair[1];this.ctx.acceptWebSocket(server);
    const s:CompletionAttachment={channel:'completion',room:gen,self,nonce,roster:i.roster,expires:now+540000,sequence:0,at:0,payload:'',rate:[]};server.serializeAttachment(s);
    server.send(JSON.stringify({type:'welcome',self,roster:i.roster,features:['critter-completion-v1']}));
    for(const peer of this.completionSockets(gen)){const prior=peer.deserializeAttachment() as CompletionAttachment;if(this.validCompletion(prior,now)){const frame=completionFrame(prior,now);if(frame)server.send(JSON.stringify(frame));}}
    await this.schedule();return new Response(null,{status:101,webSocket:pair[0]});
   }
   if(url.pathname==='/v1/sync'&&req.headers.get('Upgrade')?.toLowerCase()==='websocket'){
    const i=identity(req.headers.get('X-SafariUtils-Lobby'),req.headers.get('X-SafariUtils-Player'),req.headers.get('X-SafariUtils-Roster')?.split(','));check(i,i.self===self);
    const gen=text(req.headers.get('X-SM-Generation'),64),nonce=text(req.headers.get('X-SM-Visit'),64),row=this.load(gen),m=row?.routing.members[self];
    check(row,!!row&&row.routing.provider==='backup'&&row.routing.lobby===i.lobby&&row.routing.roster.join()===i.roster.join()&&!!m&&!m.retired&&m.nonce===nonce&&now-row.routing.renewed<600000);
    check(row,this.ctx.getWebSockets().filter(w=>(w.deserializeAttachment() as any)?.channel!=='completion').length<40&&this.ctx.getWebSockets().length<80);
    // Carry irreversible room evidence onto each admitted attachment, including before another witness leaves.
    const priorRoom=this.room(gen),inheritedUnion=priorRoom?.union()??[0,0,0,0];
    const inheritedEvidence=[...new Map((priorRoom?.sessions.flatMap(s=>s.evidence)??[]).map(e=>[e.reporter+':'+e.source,e])).values()];
    // Only one active backup connection per current registration.
    for(const old of this.sockets(gen)){const prior=old.deserializeAttachment() as Attachment;if(prior.self===self){prior.expires=0;old.serializeAttachment(prior);old.close(1000,'replaced');}}
    const pair=new WebSocketPair(),client=pair[0],server=pair[1];this.ctx.acceptWebSocket(server);
    const s=attachment(gen,self,nonce,i.roster,now);
    if(transport===2||transport===3)
     s.protocol={connectionId:crypto.randomUUID(),ready:false,completion:false,efficient:transport===3,origin:url.origin};
    const retained=priorRoom?.sessions.find(peer=>peer.self===self&&peer.nonce===nonce);
    if(s.protocol&&retained)s.snapshots=retained.snapshots;
    s.union=inheritedUnion;s.evidence=structuredClone(inheritedEvidence);server.serializeAttachment(s);
    server.send(JSON.stringify({type:'welcome',self,roster:i.roster,features:['hotspots-v1','biome-reports-v1','room-sparklings-v1','failover-bootstrap-v1',
     ...(s.protocol?['run-reconciliation-v1','critter-completion-v1']:[]),
     ...(s.protocol?.efficient?['efficient-transport-v1']:[])],connectionId:s.protocol?.connectionId}));
    const room=this.room(gen)!;this.persist(room);this.deliver(room,[{type:'reconciling'}]);if(!s.protocol)for(const frame of room.replay(s))server.send(JSON.stringify(frame));
    await this.schedule();return new Response(null,{status:101,webSocket:client});
   }
   return json({error:'not found'},404);
  }catch{return json({error:'invalid request'},400);}
 }
 async webSocketMessage(ws:WebSocket,message:string|ArrayBuffer){
  try{
   const c=ws.deserializeAttachment() as CompletionAttachment;
   if(c?.channel==='completion'){
    check(message,typeof message==='string'&&this.validCompletion(c,Date.now()));receiveCompletion(c,message as string,Date.now());ws.serializeAttachment(c);
    const frame=completionFrame(c,Date.now());for(const peer of this.completionSockets(c.room)){const p=peer.deserializeAttachment() as CompletionAttachment;if(this.validCompletion(p,Date.now()))peer.send(JSON.stringify(frame));else peer.close(1008,'visit ended');}return;
   }
   check(message,typeof message==='string');
   const s=ws.deserializeAttachment() as Attachment,now=Date.now();
   check(s,s.expires>now&&this.sockets(s.room).includes(ws));
   const room=this.room(s.room);check(room,!!room);
   const member=room!.route.members[s.self];check(s,!!member&&!member.retired&&member.nonce===s.nonce&&room!.route.roster.join()===s.roster.join());
   const current=room!.sessions.find(x=>x.self===s.self&&x.nonce===s.nonce)!;
   check(message,new TextEncoder().encode(message).length<=4096);
   const envelope=object(JSON.parse(message));
   if(envelope.type==='renew'){
    check(s,s.protocol?.efficient===true&&!!s.protocol.origin);
    const owner=await verifyToken(text(envelope.token,4096),this.env.SIGNING_SECRET,s.protocol!.origin!,now);
    const live=ws.deserializeAttachment() as Attachment;
    if(live.expires<=Date.now()||!this.sockets(s.room).includes(ws))return;
    check(owner,owner===live.self);
    if(live.expires-now<=120_000){
     live.expires=now+600_000;
     ws.serializeAttachment(live);
     await this.schedule();
    }
    ws.send(JSON.stringify({type:'renewed',expiresInMillis:live.expires-now}));
    return;
   }
   let messages=[message];
   if(envelope.type==='batch'){
    check(s,s.protocol?.efficient===true&&current.bootstrap.stage==='done');
    const frames=envelope.frames;
    check(frames,Array.isArray(frames)&&frames.length>=1&&frames.length<=8);
    messages=(frames as unknown[]).map(frame=>{
     const child=object(frame);
     check(child,['A','N','U','C','F'].includes(String(child.kind)));
     return JSON.stringify(child);
    });
   }
   const results=messages.map((raw,index)=>room!.receive(current,raw,now,index===0));
   this.persist(room!);
   this.deliverReconciliation(ws,room!,current,results.flatMap(result=>result.private),now);
   const outgoing=results.flatMap(result=>result.broadcast);
   // Each child can produce the same room union. It is unchanged throughout a routine batch.
   this.deliver(room!,outgoing.filter((frame,index)=>index===0||(frame as {type:string}).type!=='sparklings'));
   if(results.some(result=>result.leave))ws.close(1000,'departed');
   await this.schedule();
  }catch{ws.close(1008,'invalid protocol');}
 }
 async webSocketClose(ws:WebSocket){const s=ws.deserializeAttachment() as Attachment;if((s as any)?.channel==='completion'){await this.schedule();return;}const r=this.room(s.room);if(r&&!this.sockets(s.room).some(other=>other!==ws&&(other.deserializeAttachment() as Attachment).self===s.self)){r.allocation.disconnect(s.self);this.save(r.route,r.allocation);}await this.schedule();}
 async webSocketError(ws:WebSocket){ws.close(1011,'transport');await this.webSocketClose(ws);}
 async alarm(){const now=Date.now();const visited=new Set<string>();for(const ws of this.ctx.getWebSockets()){
  const s=ws.deserializeAttachment() as Attachment;if(now>=s.expires){ws.close(1000,'renew lease');continue;}if((s as any)?.channel==='completion'){if(!this.validCompletion(s as unknown as CompletionAttachment,now))ws.close(1008,'visit ended');continue;}if(visited.has(s.room))continue;visited.add(s.room);
  const r=this.room(s.room);if(r){r.allocation.allocate(now,r.ready);this.persist(r);this.deliver(r,[]);}}
  await this.schedule();
 }
}
