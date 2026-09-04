package com.orientsec.easysocket

import com.orientsec.easysocket.session.Session
import com.orientsec.easysocket.error.EasyException

/**
 * A default implementation of [ConnectionListener] that provides empty implementations
 * for all callback methods.
 *
 * This class can be extended to create custom connection listeners where only specific
 * callback methods need to be overridden.
 */
open class DefaultConnectionListener : ConnectionListener {
    override fun onConnecting(session: Session) {
    }

    override fun onConnected(session: Session) {
    }

    override fun onConnectFailed(session: Session, e: EasyException) {
    }

    override fun onAvailable(session: Session) {
    }

    override fun onDisconnected(session: Session, e: EasyException) {
    }
}