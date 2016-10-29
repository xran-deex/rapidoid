package org.rapidoid.jdbc;

/*
 * #%L
 * rapidoid-integration-tests
 * %%
 * Copyright (C) 2014 - 2016 Nikolche Mihajlovski and contributors
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

import org.junit.Test;
import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.config.Conf;
import org.rapidoid.http.IsolatedIntegrationTest;
import org.rapidoid.setup.App;
import org.rapidoid.sql.JDBC;

@Authors("Nikolche Mihajlovski")
@Since("5.2.5")
public class SqlRoutesTest extends IsolatedIntegrationTest {

	@Test
	public void testSqlRoutes() {
		String all = "SELECT * FROM nums";
		String add = "insert into nums values (3, 'three')";

		App.run("/nums <= " + all, "POST /add <= " + add);

		JDBC.execute("create table nums (id int, name varchar(10))");

		JDBC.execute("insert into nums values (?, ?)", 1, "one");
		JDBC.execute("insert into nums values (?, ?)", 2, "two");

		isFalse(Conf.SQL.isEmpty());

		eq(Conf.SQL.get("/nums"), all);
		eq(Conf.SQL.get("POST /add"), add);

		onlyPost("/add");
		onlyGet("/nums");
	}

}
