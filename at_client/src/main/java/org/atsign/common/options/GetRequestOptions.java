package org.atsign.common.options;


/**
 * Data class used to model options to the {@link org.atsign.client.api.AtClient} get methods
 */
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
