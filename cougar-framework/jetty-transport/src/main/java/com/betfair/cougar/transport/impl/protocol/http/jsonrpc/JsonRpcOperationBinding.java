/*
 * Copyright 2013, The Sporting Exchange Limited
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
 */

package com.betfair.cougar.transport.impl.protocol.http.jsonrpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

import com.betfair.cougar.core.api.ev.OperationDefinition;
import com.betfair.cougar.core.api.ev.OperationKey;
import com.betfair.cougar.core.api.transcription.Parameter;
import com.betfair.cougar.core.api.transcription.ParameterType;

/**
 * This class represents the binding between a JsonRpc operation and an operation definition
 *
 */
public class JsonRpcOperationBinding {
		
	private final String jsonRpcMethod;
    private final OperationDefinition operationDefinition;
    private final JsonRpcParam [] jsonRpcParams;
    
    public JsonRpcOperationBinding(OperationDefinition def) {
    	this.operationDefinition = def;
    	this.jsonRpcMethod = buildMethod(def.getOperationKey());
    	jsonRpcParams = new JsonRpcParam[def.getParameters().length];
    	for (int i=0;i<def.getParameters().length;i++) {
    		jsonRpcParams[i] = new JsonRpcParam(def.getParameters()[i]);
    	}
    }
    
    public String getJsonRpcMethod() {
		return jsonRpcMethod;
	}

	public OperationDefinition getOperationDefinition() {
		return operationDefinition;
	}

	public JsonRpcParam[] getJsonRpcParams() {
		return jsonRpcParams;
	}

	private String buildMethod(OperationKey key) {
        StringBuffer sb = new StringBuffer();
        sb.append(key.getServiceName());
        sb.append("/v");
        sb.append(key.getVersion().getMajor());
        sb.append(".");
        sb.append(key.getVersion().getMinor());
        sb.append("/");
        sb.append(key.getOperationName());

		return  sb.toString().toLowerCase();
	}
	
	   
    public class JsonRpcParam {
    	private final JavaType javaType;
    	private final Parameter param;
    	
    	private JsonRpcParam (Parameter param){
    		this.param = param;
    		this.javaType = buildJavaType(param.getParameterType());
    	}
    	
    	public String getName() {
    		return param.getName();
    	}
    	public boolean isMandatory() {
    		return param.isMandatory();
    	}
    	public JavaType getJavaType() {
    		return javaType;
    	}
    }
    
    
    
    
    private static JavaType buildJavaType(ParameterType paramType) {		
		return paramType.transform(new ParameterType.TransformingVisitor<JavaType>() {
			@Override
			public JavaType transformMapType(JavaType keyType, JavaType valueType) {
				return TypeFactory.mapType(HashMap.class, keyType, valueType);
			}
			@Override
			public JavaType transformListType(JavaType elemType) {
				return TypeFactory.collectionType(ArrayList.class, elemType);
			}
			@Override
			public JavaType transformSetType(JavaType elemType) {
				return TypeFactory.collectionType(HashSet.class, elemType);
			}
			@Override
			public JavaType transformType(ParameterType.Type type, Class implementationClass) {
				return TypeFactory.fastSimpleType(implementationClass);
			}
		});
		
	}
   
}

