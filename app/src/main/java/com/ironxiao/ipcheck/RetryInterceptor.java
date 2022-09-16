package com.ironxiao.ipcheck;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RetryInterceptor implements Interceptor {
    private int maxRetry;
    private int retryCount = 1;

    public RetryInterceptor(int maxRetry) {
        this.maxRetry = maxRetry;
    }


    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        while (!response.isSuccessful() && retryCount < maxRetry) {
            retryCount++;
            if (response != null) {
                response.body().close();
                response.close();
            }
            response = chain.proceed(request);
        }
        return response;
    }
}
