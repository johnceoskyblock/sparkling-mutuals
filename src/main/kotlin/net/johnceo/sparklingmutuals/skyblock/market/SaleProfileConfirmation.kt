package net.johnceo.sparklingmutuals.skyblock.market

import net.minecraft.network.chat.Component

internal class SaleProfileConfirmation {
    private var active=false
    private var readyAt: Long?=null
    private var sentAt: Long?=null
    private var pendingUntil: Long?=null
    private var drainUntil=Long.MIN_VALUE
    private var lastNow: Long?=null
    private var attempts=0
    fun next(active: Boolean,verified: Boolean,now: Long): Boolean {
        if(!time(now))return false
        if(!active || verified) {
            if(this.active)reset(now)
            return false
        }
        if(!this.active) {
            this.active=true
            readyAt=maxOf(readyAt ?: Long.MIN_VALUE,now+1000)
        }
        if(attempts>=3 || now<(readyAt ?: Long.MAX_VALUE) || pendingUntil?.let {now<=it}==true)return false
        attempts++;sentAt=now;pendingUntil=now+15000;drainUntil=now+15001;readyAt=now+30000
        return true
    }
    fun accept(message: Component,now: Long): String? {
        if(!time(now) || !active || pendingUntil?.let {now<=it}!=true)return null
        val id=SaleProfileEvidence.profile(message) ?: return null
        pendingUntil=null
        return id
    }
    fun passiveLegacy(message: Component,now: Long): String? {
        if(!time(now) || now<drainUntil || pendingUntil!=null)return null
        return SaleProfileEvidence.profile(message.string)
    }
    /** Drain any old request before accepting a response in a replacement context. */
    fun reset(now: Long) {
        val effective=maxOf(now,lastNow ?: now)
        readyAt=maxOf(effective+1000,drainUntil,
            sentAt?.plus(5000) ?: Long.MIN_VALUE)
        active=false;pendingUntil=null;attempts=0;lastNow=effective
    }
    private fun time(now: Long): Boolean {
        val previous=lastNow
        if(previous!=null && now<previous) {pendingUntil=null;return false}
        lastNow=now
        return true
    }
}
