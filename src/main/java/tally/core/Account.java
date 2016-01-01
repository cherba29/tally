package tally.core;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Account {
  public enum Type {
    BILL("bill"),
    CHECKING("checking"),
    CREDIT("credit"),
    CREDIT_CARD("credit-card"),
    DEFERRED_INCOME("deferred income"),
    EXTERNAL("external"),
    INCOME("income"),
    INVESTMENT("investment"),
    RETIREMENT("retirement"),
    SUMMARY("_summary_"),
    TAX("tax_");

    private static final Map<String, Type> stringToEnum = new HashMap<>();
    static {
      for (Type op : values()) {
        stringToEnum.put(op.name, op);
      }
    }

    private final String name;

    Type(String name) {
      this.name = name;
    }

    @Nullable
    public static Type fromString(String symbol) {
      Type type = stringToEnum.get(symbol);
      return type;
    }
  }

  private final String name;
  private final Type type;

  @Nullable private final String description;
  @Nullable private final String number;
  private final ImmutableList<String> owners;
  @Nullable private final Month openedOn;
  @Nullable private final Month closedOn;
  @Nullable private final String url;
  @Nullable private final String userName;
  @Nullable private final String password;
  @Nullable private final String phone;
  @Nullable private final String address;

  public static class Builder {
    @Nullable private String name;
    @Nullable private String description;
    @Nullable private String number;
    @Nullable private Type type;
    private final List<String> owners;
    @Nullable private Month openedOn;
    @Nullable private Month closedOn;
    @Nullable private String url;
    @Nullable private String userName;
    @Nullable private String password;
    @Nullable  private String phone;
    @Nullable private String address;

    public Builder() {
      this.name = null;
      this.description = null;
      this.number = null;
      this.type = null;
      this.owners = new ArrayList<>();
      this.openedOn = null;
      this.closedOn = null;
      this.url = null;
      this.userName = null;
      this.password = null;
      this.phone = null;
      this.address = null;
    }

    public Builder addOwner(String owner) {
      checkNotNull(owner);
      owners.add(owner);
      return this;
    }

    public Builder addOwners(List<String> owners) {
      this.owners.addAll(owners);
      return this;
    }

    @Nullable
    public String getName() {
      return name;
    }

    public Builder setAddress(String address) {
      checkNotNull(address);
      this.address = address;
      return this;
    }

    public Builder setClosedOn(Month closedOn) {
      checkNotNull(closedOn);
      this.closedOn = closedOn;
      return this;
    }

    public Builder setName(String name) {
      checkNotNull(name);
      this.name = name;
      return this;
    }

    public Builder setDescription(String description) {
      checkNotNull(description);
      this.description = description;
      return this;
    }

    public Builder setType(Type type) {
      checkNotNull(type);
      this.type = type;
      return this;
    }

    public Builder setNumber(String number) {
      checkNotNull(number);
      this.number = number;
      return this;
    }

    public Builder setOpenedOn(Month openedOn) {
      checkNotNull(openedOn);
      this.openedOn = openedOn;
      return this;
    }

    public Builder setPassword(String password) {
      checkNotNull(password);
      this.password = password;
      return this;
    }

    public Builder setPhone(String phone) {
      checkNotNull(phone);
      this.phone = phone;
      return this;
    }

    public Builder setUrl(String url) {
      checkArgument(url != null);
      this.url = url;
      return this;
    }

    public Builder setUserName(String userName) {
      checkNotNull(userName);
      this.userName = userName;
      return this;
    }

    public Account build() throws BudgetException {
      if (closedOn != null && openedOn != null && closedOn.compareTo(openedOn) < 0) {
        throw new BudgetException(String.format(
            "Account %s was closed %s before %s it was oppened", name, closedOn, openedOn));
      }
      return new Account(this);
    }
  }

  public String getName() {
    return name;
  }

  @Nullable
  public String getDescription() {
    return description;
  }

  @Nullable
  public String getNumber() {
    return number;
  }

  public Type getType() {
    return type;
  }

  /**
   * This account should not be considered in the owners summary.
   */
  public boolean isExternal() {
    return type == Type.TAX || type == Type.EXTERNAL || type == Type.DEFERRED_INCOME;
  }

  public boolean isSummary() {
    return type == Type.SUMMARY;
  }

  public List<String> getOwners() {
    ArrayList<String> list = new ArrayList<>();
    list.addAll(owners);
    return list;
  }

  public boolean hasCommonOwner(Account account) {
    for (String owner : owners) {
      if (account.owners.contains(owner)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public Month getOpenedOn() {
    return openedOn;
  }

  @Nullable
  public Month getClosedOn() {
    return closedOn;
  }

  @Nullable
  public String getUrl() {
    return url;
  }

  @Nullable
  public String getUserName() {
    return userName;
  }

  @Nullable
  public String getPassword() {
    return password;
  }

  @Nullable
  public String getPhone() {
    return phone;
  }

  @Nullable
  public String getAddress() {
    return address;
  }

  public boolean isOpen(@Nullable Month month) {
    if (month == null) {
      return closedOn == null;
    }
    if (openedOn != null && openedOn.compareTo(month) > 0) {
      return false;
    }
    return closedOn == null || closedOn.compareTo(month) >= 0;
  }

  public boolean isOpen(Collection<Month> months) {
    for (Month month : months) {
      if (isOpen(month)) {
        return true;
      }
    }
    return false;
  }

  private Account(Builder builder) {
    if (builder.name == null) {
      throw new IllegalArgumentException("Name must be set");
    }
    if (builder.type == null) {
      throw new IllegalArgumentException("Type must be set");
    }
    name = builder.name;
    type = builder.type;

    description = builder.description;
    number = builder.number;
    owners = ImmutableList.copyOf(builder.owners);
    openedOn = builder.openedOn;
    closedOn = builder.closedOn;
    url = builder.url;
    userName = builder.userName;
    password = builder.password;
    phone = builder.phone;
    address = builder.address;
  }

  @Override
  public String toString(/*Account this*/) {
    return "Account [name=" + name + ",type=" + type + "]";
  }

  public String toDebugString(int indent) {
    final char[] spaces = new char[indent + 2];
    Arrays.fill(spaces, ' ');
    final String indentString = new String(spaces);

    String[] data = new String[] {
        indentString.substring(0, indent), "Account ", name, " ", type.toString(), "\n",
        indentString, "description: ", (description == null) ? "~" : description.toString(), "\n",
        indentString, "number: ", (number == null) ? "~" : number.toString(), "\n",
        indentString, "owners: ", owners.toString(), "\n",
        indentString, "openedOn: ", (openedOn == null)? "~" : openedOn.toString(), "\n",
        indentString, "closedOn: ", (closedOn == null)? "~" : closedOn.toString(), "\n",
        indentString, "url: ", (url == null) ? "~" : url.toString(), "\n",
        indentString, "userName: ", (userName == null) ? "~" : userName.toString(), "\n",
        indentString, "password: ", (password == null) ? "~" : password.toString(), "\n",
        indentString, "phone: ", (phone == null) ? "~" : phone.toString(), "\n",
        indentString, "address: ", (address == null) ? "~" : address.toString(), "\n",
    };
    return Joiner.on("").join(data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Account)) {
      return false;
    }
    Account other = (Account) obj;
    return name.equals(other.name) && type == other.type;
  }
}
