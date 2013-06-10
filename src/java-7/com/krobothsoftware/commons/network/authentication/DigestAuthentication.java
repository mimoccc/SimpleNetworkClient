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
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 * 
 */

package com.krobothsoftware.commons.network.authentication;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Formatter;
import java.util.Locale;

import com.krobothsoftware.commons.network.RequestBuilder;
import com.krobothsoftware.commons.network.Response;
import com.krobothsoftware.commons.network.ResponseAuthenticate;
import com.krobothsoftware.commons.network.value.NameValuePair;
import com.krobothsoftware.commons.util.CommonUtils;

public class DigestAuthentication extends Authentication {
	private static final String DEFAULT_ELEMENT_CHARSET = "US-ASCII";

	/**
	 * Used to clone for thread safty.
	 */
	private static MessageDigest md5;

	private final SecureRandom rnd = new SecureRandom();
	private String nonce;
	private String realm;
	private String algorithm;
	private String qop;
	private String charset;
	private String nc;
	private int nonceCount;

	private static final char[] HEXADECIMAL = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	@Deprecated
	public DigestAuthentication(String username, String password) {
		super(username, password);
	}

	public DigestAuthentication(String username, char[] password) {
		super(username, password);
	}

	@Override
	public void setup(RequestBuilderAuthenticate request) throws IOException {
		String header = nonceUsable(request);

		if (header != null) {
			request.header(HEADER_AUTHORIZATION, header);
		}

		return;
	}

	@Override
	public Response authenticate(RequestBuilderAuthenticate request,
			ResponseAuthenticate response) throws IOException {

		String headerField = response.getAuthentication();
		response.close();
		nonceCount = 1;

		realm = getHeaderValueByType("realm", headerField);
		nonce = getHeaderValueByType("nonce", headerField);
		algorithm = getHeaderValueByType("algorithm", headerField);
		qop = getHeaderValueByType("qop", headerField);
		charset = getHeaderValueByType("charset", headerField);
		request.header(HEADER_AUTHORIZATION, createHeader(request));

		return new RequestBuilder(request).execute(networkHelper);

	}

	@Override
	public void reset() {
		nonce = null;
		realm = null;
		algorithm = null;
		qop = null;
		charset = null;
		nc = null;
		nonceCount = 0;

	}

	@Override
	public boolean authenticateSupported() {
		return true;
	}

	private String nonceUsable(RequestBuilder request) throws IOException {

		if (nonce == null) return null;
		nonceCount++;

		return createHeader(request);

	}

	@SuppressWarnings("boxing")
	private String createHeader(RequestBuilder request) throws IOException {
		MessageDigest messageDigest = null;
		StringBuffer buffer = null;

		try {
			// better than a new instance
			messageDigest = (MessageDigest) md5.clone();

			StringBuilder sb = new StringBuilder(256);
			try (Formatter formatter = new Formatter(sb, Locale.US)) {
				formatter.format("%08x", nonceCount);
			}
			nc = sb.toString();

			if (charset == null) {
				charset = DEFAULT_ELEMENT_CHARSET;
			}

			// generate client nonce
			String cnonce = createCnonce(rnd);

			// make MD5 hash of username, realm, and password
			buffer = new StringBuffer();
			buffer.append(username).append(':').append(realm).append(':')
					.append(password);
			String hash1 = buffer.toString();
			hash1 = encode(messageDigest.digest(hash1.getBytes(charset)));

			// made MD5 hash of method name and url path
			buffer = new StringBuffer();
			buffer.append(request.getMethod()).append(':')
					.append(request.getUrl().getPath());
			String hash2 = buffer.toString();
			hash2 = encode(messageDigest.digest(hash2.getBytes(charset)));

			// make MD5 hash of hash1, nonce, nc, cnonce, qop, and hash2
			buffer = new StringBuffer();
			buffer.append(hash1).append(':').append(nonce).append(':')
					.append(nc).append(':').append(cnonce).append(':')
					.append(qop).append(':').append(hash2);
			String response = buffer.toString();
			response = encode(messageDigest.digest(response.getBytes(charset)));

			// setup header

			String[] params = { "username", username, "realm", realm, "nonce",
					nonce, "uri", request.getUrl().getPath(), "algorithm",
					algorithm, "response", response, "qop", qop, "nc", nc,
					"cnonce", cnonce };

			buffer = new StringBuffer();
			buffer.append("Digest ");

			for (int i = 0; i < params.length; i += 2) {
				if (!params[i].equalsIgnoreCase("username")) buffer
						.append(", ");
				// check if need parentheses
				if (params[i].equalsIgnoreCase("nc") || params[i].equals("qop")) {
					buffer.append(NameValuePair.getPair(params[i],
							params[i + 1]));
				} else {
					buffer.append(NameValuePair.getQuotedPair(params[i],
							params[i + 1]));
				}
			}

		} catch (CloneNotSupportedException e) {
			// MD5 clone is supported
		}

		log.info("Authorizing Digest[{}] {}", String.valueOf(nonceCount),
				request.getUrl());

		return buffer.toString();
	}

	private static String getHeaderValueByType(String type, String headerText) {
		String header = headerText.replaceFirst("Digest ", "");
		header = header.replaceFirst("Basic ", "");
		String[] values = header.split(",");

		for (String value : values) {
			if (type.equalsIgnoreCase(value.substring(0, value.indexOf("="))
					.trim())) {
				return CommonUtils.trim(
						value.substring(value.indexOf("=") + 1), '"');
			}
		}

		return null;
	}

	private static String encode(byte[] binaryData) {
		int n = binaryData.length;
		char[] buffer = new char[n * 2];
		for (int i = 0; i < n; i++) {
			int low = binaryData[i] & 0x0f;
			int high = (binaryData[i] & 0xf0) >> 4;
			buffer[i * 2] = HEXADECIMAL[high];
			buffer[i * 2 + 1] = HEXADECIMAL[low];
		}

		return new String(buffer);
	}

	private static String createCnonce(SecureRandom rnd) {
		byte[] tmp = new byte[8];
		rnd.nextBytes(tmp);
		return encode(tmp);
	}

	static {
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// should always exist
		}
	}

}
