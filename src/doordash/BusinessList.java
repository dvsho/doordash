package doordash;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class BusinessList {

@SerializedName("businesses")
@Expose
private List<Business> businesses;
@SerializedName("total")
@Expose
private int total;
@SerializedName("region")
@Expose
private Region region;

public List<Business> getBusinesses() {
return businesses;
}

public void setBusinesses(List<Business> businesses) {
this.businesses = businesses;
}

public int getTotal() {
return total;
}

public void setTotal(int total) {
this.total = total;
}

public Region getRegion() {
return region;
}

public void setRegion(Region region) {
this.region = region;
}

}
