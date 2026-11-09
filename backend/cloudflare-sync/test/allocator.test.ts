import {it,expect} from 'vitest';
import {Allocation,javaHash} from '../src/allocator';
const a='0'.repeat(31)+'1',b='0'.repeat(31)+'2',c='0'.repeat(31)+'3',d='0'.repeat(31)+'4';
it('uses signed Java hash and exact report deadlines without restart',()=>{
 expect(javaHash('Aa')).toBe(2112);expect(javaHash('zzzzzz')).toBe(-685785664);
 const x=new Allocation([a,b,c,d]);for(const id of [a,b,c,d])x.hello(id,{display:true,ticketed:true,leader:id===a,present:[a,b,c,d],locked:{}},0);
 x.report(a,'FOREST',0);x.report(b,'FOREST',500);expect(x.due).toBe(5000);
 x.report(c,'CAVERN',1000);expect(x.due).toBe(3000);x.report(c,'ICY',1500);expect(x.due).toBe(3000);
 expect(x.allocate(2999)).toBe(false);expect(x.allocate(3000)).toBe(true);
 expect(new Set(Object.values(x.locks)).size).toBe(4);expect(x.locks[a]).toBe('FOREST');expect(x.fairness[a]).toBe(1);
 const before={...x.locks};x.report(d,'FOREST',3500);x.allocate(4000);expect(x.locks).toEqual(before);
});
it('imports locks without wins, freezes contradictions and gates allocation on reconciliation',()=>{
 const x=new Allocation([a,b]);x.importLocks({[a]:'ICY'});expect(x.fairness[a]??0).toBe(0);
 x.hello(b,{display:false,ticketed:true,leader:false,present:[a,b],locked:{}},0);x.report(b,'ICY',1);
 expect(x.allocate(10000,false)).toBe(false);x.allocate(10000,true);expect(x.locks[a]).toBe('ICY');expect(x.locks[b]).toBe('FOREST');
 x.importLocks({[a]:'FOREST'});expect(x.conflict).toBe(true);expect(x.snapshot(10000,a).notifications).toEqual([]);
});
it('only current leader gets non-display notices after 2s grace; acknowledgments survive restore',()=>{
 const x=new Allocation([a,b]);x.hello(a,{display:true,ticketed:true,leader:true,present:[a,b],locked:{}},0);
 x.hello(b,{display:false,ticketed:true,leader:false,present:[a,b],locked:{}},0);x.report(a,'FOREST',0);x.report(b,'CAVERN',0);x.allocate(2000);
 expect(x.snapshot(3999,a).notifications).toEqual([]);expect(x.snapshot(4000,b).notifications).toEqual([]);
 const note=x.snapshot(4000,a).notifications[0];expect(note.player).toBe(b);x.ack(a,note.id);
 const restored=Allocation.restore(x.toJSON());expect(restored.snapshot(5000,a).notifications).toEqual([]);
});
it('equal revision contradictory primary fairness freezes; repeated copies seed once',()=>{
 const x=new Allocation([a,b]);x.seed({run:'primary',revision:3,counts:{[a]:7}});x.seed({run:'primary',revision:3,counts:{[a]:7}});expect(x.fairness[a]).toBe(7);
 x.seed({run:'primary',revision:3,counts:{[a]:8}});expect(x.conflict).toBe(true);
});
it('keeps backup-only wins separate when seeding a later primary snapshot once',()=>{
 const x=new Allocation([a,b],{[a]:3});x.seed({run:'primary',revision:1,counts:{[a]:7}});x.seed({run:'primary',revision:1,counts:{[a]:7}});
 expect(x.fairness[a]).toBe(10);expect(x.backupWins[a]).toBe(3);
 const next=Allocation.restore(x.toJSON());expect(next.backupWins[a]).toBe(3);
});
it('unticketed members remain excluded and late attendance uses 250ms batching',()=>{
 const x=new Allocation([a,b,c]);x.hello(a,{display:true,ticketed:true,leader:true,present:[a,b],locked:{}},0);x.hello(b,{display:true,ticketed:false,leader:false,present:[],locked:{}},0);
 x.report(a,'FOREST',0);x.allocate(5000);expect(Object.keys(x.locks)).toEqual([a]);
 x.hello(c,{display:true,ticketed:true,leader:false,present:[],locked:{}},6000);expect(x.due).toBe(6250);expect(x.allocate(6249)).toBe(false);expect(x.allocate(6250)).toBe(true);expect(x.locks[c]).toBe('CAVERN');
});
