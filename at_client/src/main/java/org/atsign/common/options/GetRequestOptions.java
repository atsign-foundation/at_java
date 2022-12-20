package org.atsign.common.options;

public class GetRequestOptions extends RequestOptions {

    private boolean bypassCache;

    public GetRequestOptions() {

    }

    public GetRequestOptions bypassCache(boolean bypassCache) {
        this.bypassCache = bypassCache;
        return this;
    }

    public boolean getBypassCache() {
        return bypassCache;
    }

    @Override
    public RequestOptions build() {
        return (GetRequestOptions) this;
    }


}
