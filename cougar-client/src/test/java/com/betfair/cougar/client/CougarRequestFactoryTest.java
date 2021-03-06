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

package com.betfair.cougar.client;

import com.betfair.cougar.api.ExecutionContext;
import com.betfair.cougar.api.geolocation.GeoLocationDetails;
import com.betfair.cougar.core.api.ev.TimeConstraints;
import com.betfair.cougar.marshalling.api.databinding.Marshaller;
import com.betfair.cougar.util.RequestUUIDImpl;
import com.betfair.cougar.util.UUIDGeneratorImpl;
import org.apache.http.Header;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.apache.http.HttpHeaders.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Date: 30/01/2013
 * Time: 10:28
 */
public class CougarRequestFactoryTest {
    private static final String CONTENT_TYPE = "application/x-my-type";

    @Mock
    private ExecutionContext mockContext;
    @Mock
    private Message mockMessage;
    @Mock
    private Marshaller mockMarshaller;
    @Mock
    private GeoLocationDetails mockGeoLocation;
    @Mock
    private TimeConstraints mockTimeConstraints;

    private Object httpRequest = new Object();
    private List<Header> headers;
    private String postEntity;
    private String contentType;
    private String httpMethod;
    private String uri = "http://some.uri/";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = ISODateTimeFormat.dateTime();

    private TestCougarRequestFactory factory = new TestCougarRequestFactory();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        factory.setGzipCompressionEnabled(false);
        RequestUUIDImpl.setGenerator(new UUIDGeneratorImpl());

    }

    @Test
    public void shouldMakeGetRequest() {
        httpMethod = "GET";
        contentType = CONTENT_TYPE;
        when(mockMessage.getHeaderMap()).thenReturn(Collections.<String, Object>emptyMap());

        Object result = factory.create(uri, httpMethod, mockMessage, mockMarshaller, contentType, mockContext, mockTimeConstraints);

        assertSame(httpRequest, result);
        assertEquals(5, headers.size());
        assertHeadersContains(headers, ACCEPT, contentType);
        assertHeadersContains(headers, USER_AGENT, CougarRequestFactory.USER_AGENT_HEADER);
        assertHeadersContains(headers, "X-REQUEST-UUID");
        assertHeadersContains(headers, "X-RequestTime");
        assertHeadersContains(headers, "X-RequestTimeout", "0");
    }

    @Test
    public void shouldMakeGetRequestWithAllHeaders() {
        httpMethod = "GET";
        contentType = CONTENT_TYPE;
        String uuid = UUID.randomUUID().toString();
        Date date = new Date();
        when(mockMessage.getHeaderMap()).thenReturn(Collections.singletonMap("X-My-Header", (Object) "value"));
        when(mockContext.traceLoggingEnabled()).thenReturn(true);
        when(mockContext.getRequestUUID()).thenReturn(new RequestUUIDImpl(uuid));
        when(mockContext.getReceivedTime()).thenReturn(date);
        factory.setGzipCompressionEnabled(true);

        Object result = factory.create(uri, httpMethod, mockMessage, mockMarshaller, contentType, mockContext, mockTimeConstraints);

        assertSame(httpRequest, result);
        assertEquals(9, headers.size());
        assertHeadersContains(headers, ACCEPT, contentType);
        assertHeadersContains(headers, USER_AGENT, CougarRequestFactory.USER_AGENT_HEADER);
        assertHeadersContains(headers, ACCEPT_ENCODING, "gzip");
        assertHeadersContains(headers, "X-Trace-Me", "true");
        assertHeadersContains(headers, "X-REQUEST-UUID", uuid);
        assertHeadersContains(headers, "X-RequestTime");
        assertHeadersContains(headers, "X-RequestTimeout", "0");
        assertHeadersContains(headers, "X-My-Header", "value");
        assertHeadersContains(headers, "X-ReceivedTime", DATE_TIME_FORMATTER.print(date.getTime()));
    }


    @Test
    public void shouldMakePostRequest() {
        httpMethod = "POST";
        contentType = CONTENT_TYPE;
        Answer<Void> postAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                ByteArrayOutputStream os = (ByteArrayOutputStream) invocationOnMock.getArguments()[0];

                os.write("some post data".getBytes());
                return null;
            }
        };


        when(mockMessage.getHeaderMap()).thenReturn(Collections.<String, Object>emptyMap());
        doAnswer(postAnswer).when(mockMarshaller).marshall(any(ByteArrayOutputStream.class), anyObject(), anyString(), eq(true));

        Object result = factory.create(uri, httpMethod, mockMessage, mockMarshaller, contentType, mockContext, mockTimeConstraints);

        assertSame(httpRequest, result);
        assertEquals(5, headers.size());
        assertHeadersContains(headers, ACCEPT, contentType);
        assertHeadersContains(headers, USER_AGENT, CougarRequestFactory.USER_AGENT_HEADER);
        assertHeadersContains(headers, "X-REQUEST-UUID");
        assertHeadersContains(headers, "X-RequestTime");
        assertHeadersContains(headers, "X-RequestTimeout", "0");
        assertEquals("some post data", postEntity);
    }


    private void assertHeadersContains(List<Header> headers, String name, String value) {
        for (Header h : headers) {
            if (h.getName().equals(name) && (h.getValue() == null || h.getValue().equals(value))) {
                return;
            }
        }
        fail("Did not find header '" + name + "' with value '" + value + "'");
    }

    private void assertHeadersContains(List<Header> headers, String name) {
        for (Header h : headers) {
            if (h.getName().equals(name) && (h.getValue() != null)) {
                return;
            }
        }
        fail("Did not find header '" + name + "'");
    }


    private class TestCougarRequestFactory extends CougarRequestFactory<Object> {

        public TestCougarRequestFactory() {
            super(new DefaultGeoLocationSerializer(), "X-REQUEST-UUID");
        }

        @Override
        protected void addHeaders(Object o, List<Header> headers) {
            assertSame(httpRequest, o);
            CougarRequestFactoryTest.this.headers = headers;
        }

        @Override
        protected void addPostEntity(Object o, String postEntity, String contentType) {
            assertSame(httpRequest, o);
            assertSame(CougarRequestFactoryTest.this.contentType, contentType);
            CougarRequestFactoryTest.this.postEntity = postEntity;
        }

        @Override
        protected Object createRequest(String httpMethod, String uri) {
            assertSame(CougarRequestFactoryTest.this.httpMethod, httpMethod);
            assertSame(CougarRequestFactoryTest.this.uri, uri);
            return httpRequest;
        }
    }
}
