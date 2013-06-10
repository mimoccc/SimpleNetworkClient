/* ===================================================
 * Copyright 2013 Kroboth Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================== 
 */

package com.krobothsoftware.snc.sen.psn;

import com.krobothsoftware.commons.network.value.CookieMap;
import com.krobothsoftware.snc.Token;
import com.krobothsoftware.snc.sen.OnlineId;

/**
 * Token for account login.
 * 
 * @author Kyle Kroboth
 * @since SEN-PSN 1.0
 * @see com.krobothsoftware.snc.sen.psn.PlaystationNetwork#login(String, String,
 *      com.krobothsoftware.commons.progress.ProgressListener)
 */
public class PsnToken extends Token implements Jid, OnlineId {
	private static final long serialVersionUID = -1813288378729398030L;
	private final String jid;
	private String session;

	/**
	 * Builds token.
	 * 
	 * @param cookies
	 * @param jid
	 * @param sessionId
	 * @since SEN-PSN 1.0
	 */
	public PsnToken(CookieMap cookies, String jid, String sessionId) {
		super(cookies);
		this.jid = jid;
		this.session = sessionId;
	}

	/**
	 * Uses {@link PsnUtils#getPsnIdFromJid(String)} from jid.
	 * 
	 * @since SEN-PSN 1.0
	 */
	@Override
	public String getOnlineId() {
		return PsnUtils.getPsnIdFromJid(jid);
	}

	/**
	 * @since SEN-PSN 1.0
	 */
	@Override
	public String getJid() {
		return jid;
	}

	/**
	 * Gets session Id.
	 * 
	 * @return session Id
	 * @since SEN-PSN 1.0
	 */
	public String getSession() {
		return session;
	}

	/**
	 * Sets session Id.
	 * 
	 * @param sessionId
	 * @since SEN-PSN 1.0
	 */
	public void setSession(String sessionId) {
		this.session = sessionId;
	}

	/**
	 * Returns string in format "[jid='jid']".
	 * 
	 * @since SEN-PSN 1.0
	 */
	@Override
	public String toString() {
		return String.format("[jid=%s]", jid);
	}

}
