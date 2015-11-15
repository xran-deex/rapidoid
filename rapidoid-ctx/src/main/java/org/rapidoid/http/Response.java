package org.rapidoid.http;

/*
 * #%L
 * rapidoid-ctx
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Map;

import org.rapidoid.mime.MediaType;

public interface Response {

	Response content(Object content);

	Object content();

	Response code(int code);

	int code();

	Map<String, String> headers();

	Map<String, String> cookies();

	Response contentType(MediaType contentType);

	MediaType contentType();

	Response redirect(String redirect);

	String redirect();

	void done();

}
