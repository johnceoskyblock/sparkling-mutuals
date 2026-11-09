import {check,integer,object,uuid} from './protocol';
const enc=new TextEncoder();
const base64=(b:Uint8Array)=>btoa(String.fromCharCode(...b)).replaceAll('+','-').replaceAll('/','_').replace(/=+$/,'');
const decode=(s:string)=>Uint8Array.from(atob(s.replaceAll('-','+').replaceAll('_','/')),c=>c.charCodeAt(0));
async function key(secret:string){check(secret,secret.length>=32);return crypto.subtle.importKey('raw',enc.encode(secret),{name:'HMAC',hash:'SHA-256'},false,['sign','verify']);}
export async function issueToken(self:string,secret:string,origin:string,now:number):Promise<string>{
 const body=base64(enc.encode(JSON.stringify({sub:uuid(self),aud:origin,exp:now+600000})));
 return body+'.'+base64(new Uint8Array(await crypto.subtle.sign('HMAC',await key(secret),enc.encode(body))));
}
export async function verifyToken(token:string,secret:string,origin:string,now:number):Promise<string>{
 check(token,token.length<=2048);const parts=token.split('.');check(token,parts.length===2&&/^[A-Za-z0-9_-]+$/.test(parts[1]));
 check(token,await crypto.subtle.verify('HMAC',await key(secret),decode(parts[1]),enc.encode(parts[0])));
 const body=object(JSON.parse(new TextDecoder().decode(decode(parts[0]))));check(body,body.aud===origin&&integer(body.exp)>now);return uuid(body.sub);
}
