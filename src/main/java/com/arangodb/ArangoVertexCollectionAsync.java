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

import java.util.concurrent.CompletableFuture;

import com.arangodb.entity.VertexEntity;
import com.arangodb.entity.VertexUpdateEntity;
import com.arangodb.internal.ArangoExecutorAsync;
import com.arangodb.internal.InternalArangoVertexCollection;
import com.arangodb.internal.velocystream.ConnectionAsync;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.VertexCreateOptions;
import com.arangodb.model.VertexDeleteOptions;
import com.arangodb.model.VertexReplaceOptions;
import com.arangodb.model.VertexUpdateOptions;
import com.arangodb.velocystream.Response;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoVertexCollectionAsync extends
		InternalArangoVertexCollection<ArangoDBAsync, ArangoDatabaseAsync, ArangoGraphAsync, ArangoExecutorAsync, CompletableFuture<Response>, ConnectionAsync> {

	protected ArangoVertexCollectionAsync(final ArangoGraphAsync graph, final String name) {
		super(graph, name);
	}

	/**
	 * Removes a vertex collection from the graph and optionally deletes the collection, if it is not used in any other
	 * graph
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#remove-vertex-collection">API
	 *      Documentation</a>
	 * @return void
	 */
	public CompletableFuture<Void> drop() {
		return executor.execute(dropRequest(), Void.class);
	}

	/**
	 * Creates a new vertex in the collection
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Vertices.html#create-a-vertex">API Documentation</a>
	 * @param value
	 *            A representation of a single vertex (POJO, VPackSlice or String for Json)
	 * @return information about the vertex
	 */
	public <T> CompletableFuture<VertexEntity> insertVertex(final T value) {
		return executor.execute(insertVertexRequest(value, new VertexCreateOptions()),
			insertVertexResponseDeserializer(value));
	}

	/**
	 * Creates a new vertex in the collection
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Vertices.html#create-a-vertex">API Documentation</a>
	 * @param value
	 *            A representation of a single vertex (POJO, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return information about the vertex
	 */
	public <T> CompletableFuture<VertexEntity> insertVertex(final T value, final VertexCreateOptions options) {
		return executor.execute(insertVertexRequest(value, options), insertVertexResponseDeserializer(value));
	}

	/**
	 * Fetches an existing vertex
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Vertices.html#get-a-vertex">API Documentation</a>
	 * @param key
	 *            The key of the vertex
	 * @param type
	 *            The type of the vertex-document (POJO class, VPackSlice or String for Json)
	 * @return the vertex identified by the key
	 */
	public <T> CompletableFuture<T> getVertex(final String key, final Class<T> type) {
		return executor.execute(getVertexRequest(key, new DocumentReadOptions()), getVertexResponseDeserializer(type));
	}

	/**
	 * Fetches an existing vertex
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Vertices.html#get-a-vertex">API Documentation</a>
	 * @param key
	 *            The key of the vertex
	 * @param type
	 *            The type of the vertex-document (POJO class, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return the vertex identified by the key
	 */
	public <T> CompletableFuture<T> getVertex(
		final String key,
		final Class<T> type,
		final DocumentReadOptions options) {
		return executor.execute(getVertexRequest(key, options), getVertexResponseDeserializer(type));
	}

	/**
	 * Replaces the vertex with key with the one in the body, provided there is such a vertex and no precondition is
	 * violated
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Vertices.html#replace-a-vertex">API
	 *      Documentation</a>
	 * @param key
	 *            The key of the vertex
	 * @param type
	 *            The type of the vertex-document (POJO class, VPackSlice or String for Json)
	 * @return information about the vertex
	 */
	public <T> CompletableFuture<VertexUpdateEntity> replaceVertex(final String key, final T value) {
		return executor.execute(replaceVertexRequest(key, value, new VertexReplaceOptions()),
			replaceVertexResponseDeserializer(value));
	}

	/**
	 * Replaces the vertex with key with the one in the body, provided there is such a vertex and no precondition is
	 * violated
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Vertices.html#replace-a-vertex">API
	 *      Documentation</a>
	 * @param key
	 *            The key of the vertex
	 * @param type
	 *            The type of the vertex-document (POJO class, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return information about the vertex
	 */
	public <T> CompletableFuture<VertexUpdateEntity> replaceVertex(
		final String key,
		final T value,
		final VertexReplaceOptions options) {
		return executor.execute(replaceVertexRequest(key, value, options), replaceVertexResponseDeserializer(value));
	}

	/**
	 * Partially updates the vertex identified by document-key. The value must contain a document with the attributes to
	 * patch (the patch document). All attributes from the patch document will be added to the existing document if they
	 * do not yet exist, and overwritten in the existing document if they do exist there.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Vertices.html#modify-a-vertex">API Documentation</a>
	 * @param key
	 *            The key of the vertex
	 * @param type
	 *            The type of the vertex-document (POJO class, VPackSlice or String for Json)
	 * @return information about the vertex
	 * @throws ArangoDBException
	 */
	public <T> CompletableFuture<VertexUpdateEntity> updateVertex(final String key, final T value)
			throws ArangoDBException {
		return executor.execute(updateVertexRequest(key, value, new VertexUpdateOptions()),
			updateVertexResponseDeserializer(value));
	}

	/**
	 * Partially updates the vertex identified by document-key. The value must contain a document with the attributes to
	 * patch (the patch document). All attributes from the patch document will be added to the existing document if they
	 * do not yet exist, and overwritten in the existing document if they do exist there.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Vertices.html#modify-a-vertex">API Documentation</a>
	 * @param key
	 *            The key of the vertex
	 * @param type
	 *            The type of the vertex-document (POJO class, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return information about the vertex
	 * @throws ArangoDBException
	 */
	public <T> CompletableFuture<VertexUpdateEntity> updateVertex(
		final String key,
		final T value,
		final VertexUpdateOptions options) throws ArangoDBException {
		return executor.execute(updateVertexRequest(key, value, options), updateVertexResponseDeserializer(value));
	}

	/**
	 * Removes a vertex
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Vertices.html#remove-a-vertex">API Documentation</a>
	 * @param key
	 *            The key of the vertex
	 */
	public CompletableFuture<Void> deleteVertex(final String key) {
		return executor.execute(deleteVertexRequest(key, new VertexDeleteOptions()), Void.class);
	}

	/**
	 * Removes a vertex
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Vertices.html#remove-a-vertex">API Documentation</a>
	 * @param key
	 *            The key of the vertex
	 * @param options
	 *            Additional options, can be null
	 */
	public CompletableFuture<Void> deleteVertex(final String key, final VertexDeleteOptions options) {
		return executor.execute(deleteVertexRequest(key, options), Void.class);
	}

}
