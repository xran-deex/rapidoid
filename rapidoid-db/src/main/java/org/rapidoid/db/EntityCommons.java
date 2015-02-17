package org.rapidoid.db;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.util.CommonRoles;
import org.rapidoid.util.U;
import org.rapidoid.var.Var;
import org.rapidoid.var.Vars;

/*
 * #%L
 * rapidoid-db
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski
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

@Authors("Nikolche Mihajlovski")
@Since("2.2.0")
public class EntityCommons implements IEntityCommons, CommonRoles, Serializable {

	private static final long serialVersionUID = 8414835674684110203L;

	private long id;

	private long version;

	private String createdBy;

	private Date createdOn;

	private String lastUpdatedBy;

	private Date lastUpdatedOn;

	private Map<String, Object> extras;

	private Map<String, Object> tmps;

	@Override
	public synchronized long id() {
		return id;
	}

	@Override
	public synchronized void id(long id) {
		this.id = id;
	}

	@Override
	public synchronized long version() {
		return version;
	}

	@Override
	public synchronized void version(long version) {
		this.version = version;
	}

	@Override
	public synchronized String createdBy() {
		return createdBy;
	}

	@Override
	public synchronized void createdBy(String createdBy) {
		this.createdBy = createdBy;
	}

	@Override
	public synchronized Date createdOn() {
		return createdOn;
	}

	@Override
	public synchronized void createdOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public synchronized String lastUpdatedBy() {
		return lastUpdatedBy;
	}

	@Override
	public synchronized void lastUpdatedBy(String lastUpdatedBy) {
		this.lastUpdatedBy = lastUpdatedBy;
	}

	@Override
	public synchronized Date lastUpdatedOn() {
		return lastUpdatedOn;
	}

	@Override
	public synchronized void lastUpdatedOn(Date lastUpdatedOn) {
		this.lastUpdatedOn = lastUpdatedOn;
	}

	@Override
	public synchronized int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public synchronized boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;

		if (id == 0) {
			return false;
		}

		if (!(obj instanceof IEntityCommons)) {
			return false;
		}

		IEntityCommons other = (IEntityCommons) obj;
		if (id != other.id())
			return false;

		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized <K, V> ConcurrentMap<K, V> _map(String name) {
		extras();

		if (!extras.containsKey(name)) {
			extras.put(name, U.concurrentMap());
		}

		return (ConcurrentMap<K, V>) extras.get(name);
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T> List<T> _list(String name) {
		extras();

		if (!extras.containsKey(name)) {
			extras.put(name, U.list());
		}

		return (List<T>) extras.get(name);
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T> Set<T> _set(String name) {
		extras();

		if (!extras.containsKey(name)) {
			extras.put(name, U.set());
		}

		return (Set<T>) extras.get(name);
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T> Var<T> _var(String name, T defaultValue) {
		extras();

		if (!extras.containsKey(name)) {
			extras.put(name, Vars.var(defaultValue));
		}

		return (Var<T>) extras.get(name);
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T> T _extra(String name) {
		return (T) extras().get(name);
	}

	@Override
	public synchronized void _extra(String name, Object value) {
		extras().put(name, value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T> T _tmp(String name) {
		return (T) tmps().get(name);
	}

	@Override
	public synchronized void _tmp(String name, Object value) {
		tmps().put(name, value);
	}

	private synchronized Map<String, Object> extras() {
		if (extras == null) {
			extras = U.map();
		}
		return extras;
	}

	private synchronized Map<String, Object> tmps() {
		if (tmps == null) {
			tmps = U.map();
		}
		return tmps;
	}

}