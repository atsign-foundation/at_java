package org.atsign.client.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class SecondaryTest {

    @Test
    public void testSetRawErrorResponseCorrectlyParsesCodeAndMessage() {
        Secondary.Response response = new Secondary.Response();
        response.setRawErrorResponse("AT0001-meaning of error code : any other text");

        assertThat(response.isError(), is(true));
        assertThat(response.getErrorCode(), equalTo("AT0001"));
        assertThat(response.getErrorText(), equalTo("any other text"));
    }
}