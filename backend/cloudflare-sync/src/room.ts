import {COMPLETION_TTL,completionPayload} from './completion';
import {Allocation,Fairness} from './allocator';
import {RouteRoom,reconciled} from './routing';
import {Biome,CODES,check,integer,locks,numbers,object,text,uuid} from './protocol';
export interface Evidence {reporter:string;source:string;nests:number;facts:number[];caught:number[]}
export interface Snapshot {sequence:number;kind:string;payload:string;catchUpOnly?:boolean}
export interface Attachment {
 room:string;self:string;nonce:string;roster:string[];expires:number;rate:number[];sequence:number;
 protocol?:{connectionId:string;visitId?:string;ready:boolean;completion:boolean;efficient?:boolean;origin?:string};
 snapshots:Record<string,Snapshot>;union:number[];evidence:Evidence[];
 bootstrap:{stage:'none'|'open'|'done';frames:number;conflict:boolean;facts:Evidence[];locks:Record<string,Biome>;instructed:Record<string,Biome>;fairness:Fairness|null};
}
export const FACT_INDICES=[23,24,25,26,27,28,29,30,31,32,34,35,36,37];
export function mergeEvidence(target:Evidence[],e:Evidence){
 const old=target.find(v=>v.reporter===e.reporter&&v.source===e.source);
 if(old){old.nests|=e.nests;FACT_INDICES.forEach(i=>old.facts[i]=i===24?Math.max(old.facts[i],e.facts[i]):old.facts[i]|e.facts[i]);old.caught=old.caught.map((v,i)=>v|e.caught[i]);}
 else target.push(structuredClone(e));check(target,target.length<=8);
}
export function attachment(room:string,self:string,nonce:string,roster:string[],now:number):Attachment{
 return {room,self,nonce,roster,expires:now+600000,rate:[],sequence:0,snapshots:{},union:[0,0,0,0],evidence:[],bootstrap:{stage:'none',frames:0,conflict:false,facts:[],locks:{},instructed:{},fairness:null}};
}
export function evidence(input:unknown,roster:string[]):Evidence{
 const v=object(input),reporter=uuid(v.reporter),source=v.source??'reporter';check(v,roster.includes(reporter)&&(source==='reporter'||source==='room'));const nests=integer(v.nests,0,8191);
 check(v,Array.isArray(v.facts)&&v.facts.length===38);const facts=v.facts as number[];
 // Bootstrap may contain irreversible facts only, never inventory/feeder state or positive sightings.
 const probe=[...facts];probe[17]=-1;probe[33]=2;numbers('A',probe.join(','));
 facts.forEach((n,i)=>{integer(n);check(n,FACT_INDICES.includes(i)||n===0);});
 check(v,Array.isArray(v.caught)&&v.caught.length===2);const caught=[integer(v.caught[0],0,1073741823),integer(v.caught[1],0,127)];
 return {reporter,source:source as string,nests,facts:[...facts],caught};
}
export class Room {
 constructor(public route:RouteRoom,public allocation:Allocation,public sessions:Attachment[]){
  if(route.carry){this.mergeUnion([0,0,...route.carry.caught]);for(const s of sessions)for(const e of route.carry.evidence)mergeEvidence(s.evidence,e);}
 }
 private get bootstrapped(){return reconciled(this.route)&&Object.values(this.route.members).every(m=>m.retired||m.completed)&&this.sessions.every(s=>this.route.members[s.self]?.retired||s.bootstrap.stage==='done');}
 get replayReady(){return this.bootstrapped;}
 get ready(){return this.bootstrapped&&!this.allocation.conflict;}
 union(){const result=[0,0,...(this.route.caught??[0,0])];if(this.route.carry)for(let i=0;i<2;i++)result[i+2]|=this.route.carry.caught[i];for(const s of this.sessions)for(let i=0;i<4;i++)result[i]|=s.union[i];return result;}
 private mergeUnion(values:number[]){for(const s of this.sessions)for(let i=0;i<4;i++)s.union[i]|=values[i];}
 receive(s:Attachment,raw:string,now:number,charge=true):{broadcast:unknown[];private:unknown[];leave:boolean}{
  check(raw,new TextEncoder().encode(raw).length<=4096&&now<s.expires);if(charge){s.rate=s.rate.filter(t=>now-t<10000);check(raw,s.rate.length<40);s.rate.push(now);}
  const v=object(JSON.parse(raw)),sequence=integer(v.sequence,1);check(v,sequence>s.sequence);s.sequence=sequence;
  const kind=text(v.kind,1),payload=text(v.payload,3072);check(v,v.sender===undefined);
  if(v.catchUpOnly!==undefined)check(v,kind==='A'&&typeof v.catchUpOnly==='boolean');
  const broadcast:unknown[]=[],privateFrames:unknown[]=[];let leave=false;
  if(kind==='F'&&s.protocol?.efficient&&s.bootstrap.stage==='done'){
   const refresh=object(JSON.parse(payload));
   integer(refresh.version,1,1);
   check(refresh,s.protocol.completion&&
    refresh.runId===this.allocation.run&&refresh.visitId===s.protocol.visitId);
   const revision=integer(refresh.revision,1),record=this.route.completion?.[s.self];
   if(!s.protocol.ready||!this.replayReady)return {broadcast,private:privateFrames,leave};
   if(record?.sequence===revision&&now-record.at<COMPLETION_TTL){
    record.at=now;
    broadcast.push({type:'state',sender:s.self,kind:'C',sequence,ageMillis:0,payload:record.payload});
   }else privateFrames.push({type:'completion-resync',runId:this.allocation.run,visitId:s.protocol.visitId});
  } else if(kind==='F'){
   check(s,++s.bootstrap.frames<=12);
   const f=object(JSON.parse(payload));integer(f.version,1,1);const action=text(f.action);
   if(action==='begin'){check(f,s.bootstrap.stage==='none');s.bootstrap.stage='open';}
   else {check(f,s.bootstrap.stage==='open');
    if(action==='facts'){check(f,s.bootstrap.facts.length<8);const e=evidence(f,this.route.roster);check(e,!s.bootstrap.facts.some(old=>old.reporter===e.reporter&&old.source===e.source));s.bootstrap.facts.push(e);}
    else if(action==='locks'){s.bootstrap.locks=locks(f.locked,this.route.roster);s.bootstrap.instructed=locks(f.instructed,this.route.roster);check(f,f.conflict===undefined||typeof f.conflict==='boolean');s.bootstrap.conflict||=f.conflict===true;}
    else if(action==='fairness'){const o=object(f.fairness),counts=object(o.counts),clean:Record<string,number>={};for(const [id,n]of Object.entries(counts)){check(id,this.route.roster.includes(uuid(id)));clean[id]=integer(n,0,1000000);}s.bootstrap.fairness={run:text(o.run),revision:integer(o.revision),counts:clean};}
    else if(action==='done'){
     this.allocation.importLocks(s.bootstrap.locks);this.allocation.importLocks(s.bootstrap.instructed);Object.assign(this.allocation.instructed,s.bootstrap.instructed);
     for(const id of Object.keys(this.allocation.locks))this.allocation.assignedAt[id]??=now;
     this.allocation.conflict||=s.bootstrap.conflict;
     if(s.bootstrap.fairness)this.allocation.seed(s.bootstrap.fairness);
     s.bootstrap.stage='done';const member=this.route.members[s.self];check(member,!!member&&member.nonce===s.nonce&&!member.retired);member.completed=true;
     for(const e of s.bootstrap.facts){this.mergeUnion([0,0,...e.caught]);for(const other of this.sessions)mergeEvidence(other.evidence,e);broadcast.push({type:'bootstrap-facts',payload:e});}
    }else check(action,false);
   }
  } else {
   check(s,s.bootstrap.stage==='done');
   if(kind==='H'){
    const h=object(JSON.parse(payload));integer(h.version,1,1);
    if(s.protocol&&h.action!=='hello'){
     check(h,h.visitId===s.protocol.visitId&&
      (h.runId===this.allocation.run||(h.action==='report'&&!s.protocol.ready&&h.runId===null)));
    }
    switch(h.action){
     case 'hello': {
      if(s.protocol){
       const visit=text(h.visitId,36);
       check(h,/^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(visit));
       check(h,(!s.protocol.visitId||s.protocol.visitId===visit)&&Array.isArray(h.capabilities)&&h.capabilities.includes('run-reconciliation-v1'));
       const member=this.route.members[s.self];
       check(h,!member.protocolVisit||member.protocolVisit===visit);
       member.protocolVisit=visit;
       s.protocol.visitId=visit;
       s.protocol.completion=h.capabilities.includes('critter-completion-v1');
       if(!s.protocol.ready){
        const resumed=h.resumeRunId===this.allocation.run;
        const reconciled=this.replayReady&&(resumed||h.resumeRunId===null);
        privateFrames.push({type:'reconcile',version:1,connectionId:s.protocol.connectionId,visitId:visit,
         status:reconciled?(resumed?'resumed':'joined'):'pending',runId:reconciled?this.allocation.run:null,
         ...(!reconciled&&s.protocol.efficient?{retryAfterMillis:4000}:{})});
        if(reconciled)s.protocol.ready=true;
        else if(!resumed&&h.resumeRunId!==null)break;
       }
      }
      this.allocation.hello(s.self,h,now);break;
     }
     case 'report':{const player=uuid(h.player);check(h,this.route.roster.includes(player));const b=text(h.biome) as Biome;this.allocation.report(player,b,now);break;}
     case 'ack':this.allocation.ack(s.self,text(h.id,200));break;
     case 'leave':this.route.members[s.self].retired=true;this.allocation.leave(s.self);leave=true;break;
     default:check(h,false);
    }
   } else if(kind==='C'){
    check(s,s.protocol?.completion===true&&!!s.protocol.visitId);
    const record={sequence,payload:completionPayload(payload,{runId:this.allocation.run,visitId:s.protocol!.visitId!}),at:now};
    if(s.protocol!.ready&&this.replayReady){
     this.route.completion??={};
     this.route.completion[s.self]=record;
     broadcast.push({type:'state',sender:s.self,kind:'C',sequence,payload:record.payload,ageMillis:0});
    }
   } else {
    if(kind==='B')check(payload,CODES.includes(payload));else numbers(kind,payload);
    const snap={sequence,kind,payload,...(v.catchUpOnly?{catchUpOnly:true}: {})};s.snapshots[kind==='B'?'B'+payload:kind]=snap;
    if(kind==='S')this.mergeUnion(numbers(kind,payload));
    else {
     this.route.retained??={};
     (this.route.retained[s.self]??={})[kind==='B'?'B'+payload:kind]=snap;
    }
    if(!v.catchUpOnly&&(kind!=='S'||this.ready))broadcast.push({type:'state',sender:s.self,sequence,kind,payload});
   }
  }
  // The bounded roster-revision bridge is retired after current attachments carry its evidence.
  if(this.route.carry&&this.bootstrapped&&this.sessions.length)delete this.route.carry;
  const caught=this.union().slice(2);
  if(caught.some(Boolean))this.route.caught=caught;
  this.allocation.allocate(now,this.ready);
  // Atomic room union always precedes S relay and may only exist after the fixed cohort reconciles.
  if(this.ready)broadcast.unshift({type:'sparklings',payload:this.union().join(',')});
  return {broadcast,private:privateFrames,leave};
 }
 replay(recipient:Attachment,now=Date.now()){
  const completion=recipient.protocol?.completion?Object.entries(this.route.completion??{}).flatMap(([sender,e])=>
   sender!==recipient.self&&!this.route.members[sender]?.retired&&now-e.at<COMPLETION_TTL?
    [{type:'state',kind:'C',sender,sequence:e.sequence,payload:e.payload,ageMillis:Math.max(0,now-e.at)}]:[]):[];
  const frames:unknown[]=[];if(this.ready&&recipient.bootstrap.stage==='done')frames.push({type:'sparklings',payload:this.union().join(',')});
  const seen=new Set<string>();for(const s of this.sessions){for(const e of s.evidence){const key=JSON.stringify(e);if(!seen.has(key)){seen.add(key);frames.push({type:'bootstrap-facts',payload:e});}}
   if(s.self===recipient.self||recipient.protocol)continue;for(const v of Object.values(s.snapshots)){if(v.kind==='S'&&!this.ready)continue;frames.push({type:'state',sender:s.self,sequence:v.sequence,kind:v.kind,payload:v.payload});}}
  if(recipient.protocol)for(const [sender,snapshots] of Object.entries(this.route.retained??{})){
   if(sender===recipient.self)continue;
   for(const snapshot of Object.values(snapshots)){
    const {sequence,kind,payload}=snapshot;
    frames.push({type:'state',sender,sequence,kind,payload,...(kind==='A'||kind==='U'?{inventoryFresh:false}:{})});
   }
  }
  return [...frames,...completion];
 }
}
