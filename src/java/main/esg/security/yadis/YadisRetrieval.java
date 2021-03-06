/*******************************************************************************
 * Copyright (c) 2011 Earth System Grid Federation
 * ALL RIGHTS RESERVED. 
 * U.S. Government sponsorship acknowledged.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of the <ORGANIZATION> nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
/**
 * 
 * Earth System Grid/CMIP5
 *
 * Date: 09/08/10
 * 
 * Copyright: (C) 2010 Science and Technology Facilities Council
 * 
 * Licence: BSD
 * 
 * $Id: YadisRetrieval.java 7513 2010-09-24 12:55:36Z pjkersha $
 * 
 * @author pjkersha
 * @version $Revision: 7513 $
 */
package esg.security.yadis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import esg.security.utils.ssl.DnWhitelistX509TrustMgr;
import esg.security.utils.ssl.exceptions.DnWhitelistX509TrustMgrInitException;
import esg.security.yadis.exceptions.XrdsParseException;
import esg.security.yadis.exceptions.YadisRetrievalException;

/**
 * Retrieve a Yadis document and parse it returning the required service
 * elements
 * 
 * @author pjkershaw
 */
public class YadisRetrieval
{	
	// Trust Manager enables DN whitelisting
	protected X509TrustManager x509TrustMgr;
	
	/**
	 * Initialise SSL connection properties
	 * 
	 * @param propertiesFile input stream for properties file
	 * @throws YadisRetrievalException 
	 */
	public YadisRetrieval(InputStream propertiesFile) throws YadisRetrievalException {
		
		// Create trust manager with given whitelist and keystore settings
		// read from properties file
		try {
			x509TrustMgr = new DnWhitelistX509TrustMgr(propertiesFile);
			
		} catch (DnWhitelistX509TrustMgrInitException e) {
			throw new YadisRetrievalException("Creating trust manager", e);
		}
	}
	
	/**
	 * Initialise from an existing trust manager
	 * @param x509TrustMgr
	 */
	public YadisRetrieval(X509TrustManager x509TrustMgr) {
		this.x509TrustMgr = x509TrustMgr;
	}
	
	/**
	 * Retrieve XRD document from Yadis endpoint
	 * 
	 * @param yadisURL URL to retrieve content from
	 * @return string containing the XRD document at the given URL
	 * @throws YadisRetrievalException
	 */
	public String retrieve(URL yadisURL) throws YadisRetrievalException 
	{		
		SSLContext ctx = null;
		try {
			ctx = SSLContext.getInstance("SSL");
			
		} catch (NoSuchAlgorithmException e) {
			throw new YadisRetrievalException("Getting SSL context", e);
		}
		
		X509TrustManager tm[] = {x509TrustMgr};
		try {
			ctx.init(null, tm, null);
		} catch (KeyManagementException e) {
			throw new YadisRetrievalException("Initialising SSL context", e);
		}
		
		SSLSocketFactory socketFactory = ctx.getSocketFactory();
		HttpsURLConnection connection = null;
		try {
			connection = (HttpsURLConnection)yadisURL.openConnection();
		} catch (IOException e) {
			throw new YadisRetrievalException("Making connection", e);
		}
		connection.setSSLSocketFactory(socketFactory);
				
		InputStream ins = null;
		try {
			ins = connection.getInputStream();
		} catch (IOException e) {
			throw new YadisRetrievalException("Getting input stream", e);
		}
	    InputStreamReader isr = new InputStreamReader(ins);
	    BufferedReader in = new BufferedReader(isr);
	    StringBuffer buf = new StringBuffer();
	    String inputLine = null;

	    try {
			while ((inputLine = in.readLine()) != null)
			{
			    buf.append(inputLine);
			    buf.append(System.getProperty("line.separator"));
			}
			in.close();
		} catch (IOException e) {
			throw new YadisRetrievalException("Reading content", e);
		}

	    return buf.toString();
	}
	
	/**
	 *  Retrieve and parse Yadis document returning the services it references
	 *  
	 * @param yadisURL URL to retrieve content from
	 * @param targetTypes retrieve only this subset of target (service types).
	 * See to null to retrieve all types.
	 * @return list of services for this Yadis endpoint
	 * @throws XrdsParseException error parsing XRD document
	 * @throws YadisRetrievalException error GETing the content
	 */
	public List<XrdsServiceElem> retrieveAndParse(URL yadisURL, 
			Set<String> targetTypes) throws 
		XrdsParseException, YadisRetrievalException
	{
		String yadisDocContent;
		yadisDocContent = retrieve(yadisURL);

		XrdsDoc xrdsDoc = new XrdsDoc();
		List<XrdsServiceElem> serviceElems = xrdsDoc.parse(yadisDocContent, 
															targetTypes);
		return serviceElems;
	}
}
