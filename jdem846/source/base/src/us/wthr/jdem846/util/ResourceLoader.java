/*
 * Copyright (C) 2011 Kevin M. Gill
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
 */

package us.wthr.jdem846.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import us.wthr.jdem846.exception.ResourceLoaderException;

@Deprecated
public class ResourceLoader
{
	
	public static final int SCHEME_FILE = 1;
	public static final int SCHEME_JAR = 2;
	
	protected ResourceLoader()
	{
		
	}
	
	public static Reader getResourceAsReader(String url) throws ResourceLoaderException
	{
		ResourcePointer resourcePointer = new ResourcePointer(url);
		if (resourcePointer.getSchemeType() == SCHEME_FILE) {
			try {
				return new BufferedReader(new FileReader(resourcePointer.getUri()));
				
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ResourceLoaderException(url, "Failed to load resource at " + url, ex);
			}
		} else if (resourcePointer.getSchemeType() == SCHEME_JAR) {
			try {
				return new InputStreamReader(ResourceLoader.class.getResourceAsStream(resourcePointer.getUri()));
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ResourceLoaderException(url, "Failed to load resource at " + url, ex);
			}
		} else {
			throw new ResourceLoaderException(url, "Unsupported resource scheme: " + resourcePointer.getScheme());
		}
	}
	
	public static InputStream getResourceAsStream(String url) throws ResourceLoaderException
	{
		
		ResourcePointer resourcePointer = new ResourcePointer(url);
		if (resourcePointer.getSchemeType() == SCHEME_FILE) {
			File f = new File(resourcePointer.getUri());
			
			if (!f.exists()) 
				throw new ResourceLoaderException(url, "File not found: " + url);
			
			try {
				BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(f));
				return inputStream;
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ResourceLoaderException(url, "Failed to load resource at " + url, ex);
			}
			
		} else if (resourcePointer.getSchemeType() == SCHEME_JAR) {
			try {
				return ResourceLoader.class.getResourceAsStream(resourcePointer.getUri());
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ResourceLoaderException(url, "Failed to load resource at " + url, ex);
			}
		} else {
			throw new ResourceLoaderException(url, "Unsupported resource scheme: " + resourcePointer.getScheme());
		}
		
	}
	
	public static URL getResourceURL(String url) throws ResourceLoaderException
	{
		ResourcePointer resourcePointer = new ResourcePointer(url);
		
		if (resourcePointer.getSchemeType() == SCHEME_FILE) {
			try {
				return new URL(url);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ResourceLoaderException(url, "Failed to load resource at " + url, ex);
			}
			
		} else if (resourcePointer.getSchemeType() == SCHEME_JAR) {
			try {
				return ResourceLoader.class.getResource(resourcePointer.getUri());
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ResourceLoaderException(url, "Failed to load resource at " + url, ex);
			}
		} else {
			throw new ResourceLoaderException(url, "Unsupported resource scheme: " + resourcePointer.getScheme());
		}
	}
	

}
