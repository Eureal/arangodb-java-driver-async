/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLContext;

import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.LogEntity;
import com.arangodb.entity.LogLevelEntity;
import com.arangodb.entity.UserEntity;
import com.arangodb.internal.ArangoDBConstants;
import com.arangodb.internal.ArangoExecutorAsync;
import com.arangodb.internal.CollectionCache;
import com.arangodb.internal.CollectionCache.DBAccess;
import com.arangodb.internal.DocumentCache;
import com.arangodb.internal.InternalArangoDB;
import com.arangodb.internal.velocypack.VPackConfigure;
import com.arangodb.internal.velocypack.VPackConfigureAsync;
import com.arangodb.internal.velocystream.Communication;
import com.arangodb.internal.velocystream.CommunicationAsync;
import com.arangodb.internal.velocystream.CommunicationSync;
import com.arangodb.internal.velocystream.ConnectionAsync;
import com.arangodb.internal.velocystream.ConnectionSync;
import com.arangodb.model.LogOptions;
import com.arangodb.model.UserCreateOptions;
import com.arangodb.model.UserUpdateOptions;
import com.arangodb.velocypack.VPack;
import com.arangodb.velocypack.VPackDeserializer;
import com.arangodb.velocypack.VPackInstanceCreator;
import com.arangodb.velocypack.VPackParser;
import com.arangodb.velocypack.VPackSerializer;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.Response;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoDBAsync extends InternalArangoDB<ArangoExecutorAsync, CompletableFuture<Response>, ConnectionAsync> {

	public static class Builder {

		private static final String PROPERTY_KEY_HOST = "arangodb.host";
		private static final String PROPERTY_KEY_PORT = "arangodb.port";
		private static final String PROPERTY_KEY_TIMEOUT = "arangodb.timeout";
		private static final String PROPERTY_KEY_USER = "arangodb.user";
		private static final String PROPERTY_KEY_PASSWORD = "arangodb.password";
		private static final String PROPERTY_KEY_USE_SSL = "arangodb.usessl";
		private static final String PROPERTY_KEY_V_STREAM_CHUNK_CONTENT_SIZE = "arangodb.chunksize";
		private static final String DEFAULT_PROPERTY_FILE = "/arangodb.properties";

		private String host;
		private Integer port;
		private Integer timeout;
		private String user;
		private String password;
		private Boolean useSsl;
		private SSLContext sslContext;
		private Integer chunksize;
		private final VPack.Builder vpackBuilder;
		private final CollectionCache collectionCache;
		private final VPackParser vpackParser;

		public Builder() {
			super();
			vpackBuilder = new VPack.Builder();
			collectionCache = new CollectionCache();
			vpackParser = new VPackParser();
			VPackConfigure.configure(vpackBuilder, vpackParser, collectionCache);
			VPackConfigureAsync.configure(vpackBuilder);
			loadProperties(ArangoDBAsync.class.getResourceAsStream(DEFAULT_PROPERTY_FILE));
		}

		public Builder loadProperties(final InputStream in) {
			if (in != null) {
				final Properties properties = new Properties();
				try {
					properties.load(in);
					host = getProperty(properties, PROPERTY_KEY_HOST, host, ArangoDBConstants.DEFAULT_HOST);
					port = Integer
							.parseInt(getProperty(properties, PROPERTY_KEY_PORT, port, ArangoDBConstants.DEFAULT_PORT));
					timeout = Integer.parseInt(
						getProperty(properties, PROPERTY_KEY_TIMEOUT, timeout, ArangoDBConstants.DEFAULT_TIMEOUT));
					user = getProperty(properties, PROPERTY_KEY_USER, user, null);
					password = getProperty(properties, PROPERTY_KEY_PASSWORD, password, null);
					useSsl = Boolean.parseBoolean(
						getProperty(properties, PROPERTY_KEY_USE_SSL, useSsl, ArangoDBConstants.DEFAULT_USE_SSL));
					chunksize = Integer.parseInt(getProperty(properties, PROPERTY_KEY_V_STREAM_CHUNK_CONTENT_SIZE,
						chunksize, ArangoDBConstants.CHUNK_DEFAULT_CONTENT_SIZE));
				} catch (final IOException e) {
					throw new ArangoDBException(e);
				}
			}
			return this;
		}

		private <T> String getProperty(
			final Properties properties,
			final String key,
			final T currentValue,
			final T defaultValue) {
			return properties.getProperty(key,
				currentValue != null ? currentValue.toString() : defaultValue != null ? defaultValue.toString() : null);
		}

		public Builder host(final String host) {
			this.host = host;
			return this;
		}

		public Builder port(final Integer port) {
			this.port = port;
			return this;
		}

		public Builder timeout(final Integer timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder user(final String user) {
			this.user = user;
			return this;
		}

		public Builder password(final String password) {
			this.password = password;
			return this;
		}

		public Builder useSsl(final Boolean useSsl) {
			this.useSsl = useSsl;
			return this;
		}

		public Builder sslContext(final SSLContext sslContext) {
			this.sslContext = sslContext;
			return this;
		}

		public Builder chunksize(final Integer chunksize) {
			this.chunksize = chunksize;
			return this;
		}

		public <T> Builder registerSerializer(final Class<T> clazz, final VPackSerializer<T> serializer) {
			vpackBuilder.registerSerializer(clazz, serializer);
			return this;
		}

		public <T> Builder registerDeserializer(final Class<T> clazz, final VPackDeserializer<T> deserializer) {
			vpackBuilder.registerDeserializer(clazz, deserializer);
			return this;
		}

		public <T> Builder registerInstanceCreator(final Class<T> clazz, final VPackInstanceCreator<T> creator) {
			vpackBuilder.registerInstanceCreator(clazz, creator);
			return this;
		}

		public ArangoDBAsync build() {
			return new ArangoDBAsync(asyncBuilder(), vpackBuilder.build(),
					vpackBuilder.serializeNullValues(true).build(), vpackParser, collectionCache, syncBuilder());
		}

		private CommunicationAsync.Builder asyncBuilder() {
			return new CommunicationAsync.Builder().host(host).port(port).timeout(timeout).user(user).password(password)
					.useSsl(useSsl).sslContext(sslContext).chunksize(chunksize);
		}

		private CommunicationSync.Builder syncBuilder() {
			return new CommunicationSync.Builder().host(host).port(port).timeout(timeout).user(user).password(password)
					.useSsl(useSsl).sslContext(sslContext).chunksize(chunksize);
		}

	}

	public ArangoDBAsync(final CommunicationAsync.Builder commBuilder, final VPack vpack, final VPack vpackNull,
		final VPackParser vpackParser, final CollectionCache collectionCache,
		final CommunicationSync.Builder syncbuilder) {
		super(new ArangoExecutorAsync(commBuilder.build(vpack, collectionCache), vpack, vpackNull, vpackParser,
				new DocumentCache(), collectionCache));
		final Communication<Response, ConnectionSync> cacheCom = syncbuilder.build(vpack, collectionCache);
		collectionCache.init(new DBAccess() {
			@Override
			public ArangoDatabase db(final String name) {
				return new ArangoDatabase(cacheCom, vpackNull, vpack, vpackParser, executor.documentCache(), null,
						name);
			}
		});
	}

	protected ArangoExecutorAsync executor() {
		return executor;
	}

	public void shutdown() {
		executor.communication().disconnect();
	}

	/**
	 * Returns a handler of the system database
	 * 
	 * @return database handler
	 */
	public ArangoDatabaseAsync db() {
		return db(ArangoDBConstants.SYSTEM);
	}

	/**
	 * Returns a handler of the database by the given name
	 * 
	 * @param name
	 *            Name of the database
	 * @return database handler
	 */
	public ArangoDatabaseAsync db(final String name) {
		return new ArangoDatabaseAsync(this, name);
	}

	/**
	 * creates a new database
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Database/DatabaseManagement.html#create-database">API
	 *      Documentation</a>
	 * @param name
	 *            Has to contain a valid database name
	 * @return true if the database was created successfully.
	 */
	public CompletableFuture<Boolean> createDatabase(final String name) {
		return executor.execute(createDatabaseRequest(name), createDatabaseResponseDeserializer());
	}

	/**
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Database/DatabaseManagement.html#list-of-databases">API
	 *      Documentation</a>
	 * @return a list of all existing databases
	 */
	public CompletableFuture<Collection<String>> getDatabases() {
		return executor.execute(getDatabasesRequest(db().name()), getDatabaseResponseDeserializer());
	}

	/**
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Database/DatabaseManagement.html#list-of-accessible-databases">API
	 *      Documentation</a>
	 * @return a list of all databases the current user can access
	 */
	public CompletableFuture<Collection<String>> getAccessibleDatabases() {
		return executor.execute(getAccessibleDatabasesRequest(db().name()), getDatabaseResponseDeserializer());
	}

	/**
	 * Returns the server name and version number.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/MiscellaneousFunctions/index.html#return-server-version">API
	 *      Documentation</a>
	 * @return the server version, number
	 */
	public CompletableFuture<ArangoDBVersion> getVersion() {
		return executor.execute(getVersionRequest(), ArangoDBVersion.class);
	}

	/**
	 * Create a new user. This user will not have access to any database. You need permission to the _system database in
	 * order to execute this call.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/UserManagement/index.html#create-user">API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @param passwd
	 *            The user password
	 * @return information about the user
	 */
	public CompletableFuture<UserEntity> createUser(final String user, final String passwd) {
		return executor.execute(createUserRequest(db().name(), user, passwd, new UserCreateOptions()),
			UserEntity.class);
	}

	/**
	 * Create a new user. This user will not have access to any database. You need permission to the _system database in
	 * order to execute this call.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/UserManagement/index.html#create-user">API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @param passwd
	 *            The user password
	 * @param options
	 *            Additional properties of the user, can be null
	 * @return information about the user
	 */
	public CompletableFuture<UserEntity> createUser(
		final String user,
		final String passwd,
		final UserCreateOptions options) {
		return executor.execute(createUserRequest(db().name(), user, passwd, options), UserEntity.class);
	}

	/**
	 * Removes an existing user, identified by user. You need access to the _system database.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/UserManagement/index.html#remove-user">API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @return void
	 */
	public CompletableFuture<Void> deleteUser(final String user) {
		return executor.execute(deleteUserRequest(db().name(), user), Void.class);
	}

	/**
	 * Fetches data about the specified user. You can fetch information about yourself or you need permission to the
	 * _system database in order to execute this call.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/UserManagement/index.html#fetch-user">API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @return information about the user
	 */
	public CompletableFuture<UserEntity> getUser(final String user) {
		return executor.execute(getUserRequest(db().name(), user), UserEntity.class);
	}

	/**
	 * Fetches data about all users. You can only execute this call if you have access to the _system database.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/UserManagement/index.html#list-available-users">API
	 *      Documentation</a>
	 * @return informations about all users
	 */
	public CompletableFuture<Collection<UserEntity>> getUsers() {
		return executor.execute(getUsersRequest(db().name()), getUsersResponseDeserializer());
	}

	/**
	 * Partially updates the data of an existing user. The name of an existing user must be specified in user. You can
	 * only change the password of your self. You need access to the _system database to change the active flag.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/UserManagement/index.html#update-user">API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @param options
	 *            Properties of the user to be changed
	 * @return information about the user
	 */
	public CompletableFuture<UserEntity> updateUser(final String user, final UserUpdateOptions options) {
		return executor.execute(updateUserRequest(db().name(), user, options), UserEntity.class);
	}

	/**
	 * Replaces the data of an existing user. The name of an existing user must be specified in user. You can only
	 * change the password of your self. You need access to the _system database to change the active flag.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/UserManagement/index.html#replace-user">API
	 *      Documentation</a>
	 * @param user
	 *            The name of the user
	 * @param options
	 *            Additional properties of the user, can be null
	 * @return information about the user
	 */
	public CompletableFuture<UserEntity> replaceUser(final String user, final UserUpdateOptions options) {
		return executor.execute(replaceUserRequest(db().name(), user, options), UserEntity.class);
	}

	/**
	 * Generic Execute. Use this method to execute custom FOXX services.
	 * 
	 * @param request
	 *            VelocyStream request
	 * @return VelocyStream response
	 * @throws ArangoDBException
	 */
	public CompletableFuture<Response> execute(final Request request) {
		return executor.execute(request, response -> response);
	}

	/**
	 * Returns fatal, error, warning or info log messages from the server's global log.
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AdministrationAndMonitoring/index.html#read-global-logs-from-the-server">API
	 *      Documentation</a>
	 * @param options
	 *            Additional options, can be null
	 * @return the log messages
	 */
	public CompletableFuture<LogEntity> getLogs(final LogOptions options) {
		return executor.execute(getLogsRequest(options), LogEntity.class);
	}

	/**
	 * Returns the server's current loglevel settings.
	 * 
	 * @return the server's current loglevel settings
	 * @throws ArangoDBException
	 */
	public CompletableFuture<LogLevelEntity> getLogLevel() {
		return executor.execute(getLogLevelRequest(), LogLevelEntity.class);
	}

	/**
	 * Modifies and returns the server's current loglevel settings.
	 * 
	 * @param entity
	 *            loglevel settings
	 * @return the server's current loglevel settings
	 * @throws ArangoDBException
	 */
	public CompletableFuture<LogLevelEntity> setLogLevel(final LogLevelEntity entity) {
		return executor.execute(setLogLevelRequest(entity), LogLevelEntity.class);
	}
}
