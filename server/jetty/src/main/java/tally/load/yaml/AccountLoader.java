package tally.load.yaml;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import tally.core.Account;
import tally.core.Month;
import tally.load.LoadException;


public class AccountLoader {
  static Account.Builder loadFromRecord(String accountName, Map<String, Object> data)
      throws LoadException {
    Account.Builder accountBuilder = new Account.Builder();
    accountBuilder.setName(accountName);

    @SuppressWarnings("unchecked")
    List<String> owners = (List<String>) data.get("owner");
    if (owners == null) {
      throw new LoadException("No owners defined for account " + accountName);
    }
    accountBuilder.addOwners(owners);

    String address = (String) data.get("address");
    if (address != null) {
      accountBuilder.setAddress(address);
    }

    String description = (String) data.get("desc");
    if (description != null) {
      accountBuilder.setDescription(description);
    }

    String number = (String) data.get("number");
    if (number != null) {
      accountBuilder.setNumber(number);
    }

    String password = (String) data.get("pswd");
    if (password != null) {
      accountBuilder.setPassword(password);
    }

    String phone = (String) data.get("phone");
    if (phone != null) {
      accountBuilder.setPhone(phone);
    }

    String url = (String) data.get("url");
    if (url != null) {
      accountBuilder.setUrl(url);
    }

    String username = (String) data.get("username");
    if (username != null) {
      accountBuilder.setUserName(username);
    }

    String typeStr = (String) data.get("type");
    checkNotNull(typeStr, "Account %s type is not set", accountName);
    try {
      accountBuilder.setType(Account.Type.fromString(typeStr));
    } catch (IllegalArgumentException e) {
      throw new LoadException("Failed to load account " + accountName + ": " + e.getMessage());
    }

    String closedOnStr = (String) data.get("closed_on");
    if (closedOnStr != null) {
      accountBuilder.setClosedOn(Month.valueOf(closedOnStr));
    }

    String openedOnStr = (String) data.get("opened_on");
    if (openedOnStr != null) {
      accountBuilder.setOpenedOn(Month.valueOf(openedOnStr));
    }

    return accountBuilder;
  }
}
