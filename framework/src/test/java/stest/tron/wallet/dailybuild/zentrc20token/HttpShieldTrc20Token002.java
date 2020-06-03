package stest.tron.wallet.dailybuild.zentrc20token;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.Note;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.zen.address.DiversifierT;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.ShieldNoteInfo;
import stest.tron.wallet.common.client.utils.ZenTrc20Base;

@Slf4j
public class HttpShieldTrc20Token002 extends ZenTrc20Base {

  private String httpnode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(0);
  private String httpSolidityNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private JSONObject responseContent;
  private HttpResponse response;
  private JSONObject shieldAccountInfo;
  private JSONArray noteTxs;
  private Long publicFromAmount = getRandomLongAmount();
  JSONArray shieldedReceives = new JSONArray();
  String rcm;
  String txid;


  @Test(enabled = true, description = "Get new shield account  by http")
  public void test01GetNewShieldAccountByHttp() {
    response = getNewShieldedAddress(httpnode);
    shieldAccountInfo = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(shieldAccountInfo);
    Assert.assertEquals(shieldAccountInfo.getString("sk").length(),64);
    Assert.assertEquals(shieldAccountInfo.getString("ask").length(),64);
    Assert.assertEquals(shieldAccountInfo.getString("nsk").length(),64);
    Assert.assertEquals(shieldAccountInfo.getString("ovk").length(),64);
    Assert.assertEquals(shieldAccountInfo.getString("ak").length(),64);
    Assert.assertEquals(shieldAccountInfo.getString("nk").length(),64);
    Assert.assertEquals(shieldAccountInfo.getString("ivk").length(),64);
    Assert.assertEquals(shieldAccountInfo.getString("d").length(),22);
    Assert.assertEquals(shieldAccountInfo.getString("pkD").length(),64);
    Assert.assertEquals(shieldAccountInfo.getString("payment_address").length(),81);

    response = HttpMethed.getRcm(httpnode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    rcm = responseContent.getString("value");
  }

  @Test(enabled = true, description = "Create mint parameters by http")
  public void test02MintByHttp() {
    shieldedReceives = getHttpShieldedReceivesJsonArray(shieldedReceives,publicFromAmount,shieldAccountInfo.getString("payment_address"),rcm);
    response = createShieldContractParameters(httpnode,publicFromAmount,shieldAccountInfo,shieldedReceives);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,
        zenTrc20TokenOwnerAddressString,shieldAddress,mint,responseContent
            .getString("trigger_contract_input"),maxFeeLimit,0L,0,0L,
        zenTrc20TokenOwnerKey);

    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getTransactionInfoById(httpnode,txid,true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getJSONObject("receipt").getLong("energy_usage_total") > 300000L);
    Assert.assertEquals(responseContent.getString("contract_address"),shieldAddress);
    Assert.assertEquals(responseContent.getJSONObject("receipt").getString("result"),"SUCCESS");
  }


  @Test(enabled = true, description = "Scan shield TRC20 note by http")
  public void test03ScanTrc20NodeByHttp() {
    noteTxs = scanShieldTrc20NoteByIvk(httpnode,shieldAccountInfo);
    logger.info(noteTxs.toJSONString());
    Assert.assertEquals(noteTxs.size(),1);
    Assert.assertEquals(noteTxs.getJSONObject(0)
        .getJSONObject("note").getLong("value"),publicFromAmount);
    Assert.assertEquals(noteTxs.getJSONObject(0)
        .getJSONObject("note").getString("payment_address"),
        shieldAccountInfo.getString("payment_address"));
    Assert.assertEquals(noteTxs.getJSONObject(0)
        .getString("txid"),txid);
  }

  @Test(enabled = true, description = "Get note path by http")
  public void test04GetNotePathByHttp() {
    JSONArray shieldSpends = new JSONArray();
    shieldSpends = createAndSetShieldedSpends(httpnode,shieldSpends,noteTxs.getJSONObject(0));

    logger.info(shieldSpends.toJSONString());

    response = createShieldContractParametersForBurn(httpnode,shieldAccountInfo,shieldSpends,zenTrc20TokenOwnerAddressString,publicFromAmount);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);

    txid = HttpMethed.triggerContractGetTxidWithVisibleTrue(httpnode,
        zenTrc20TokenOwnerAddressString,shieldAddress,burn,responseContent
            .getString("trigger_contract_input"),maxFeeLimit,0L,0,0L,
        zenTrc20TokenOwnerKey);


    HttpMethed.waitToProduceOneBlock(httpnode);
    response = HttpMethed.getTransactionInfoById(httpnode,txid,true);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Assert.assertTrue(responseContent.getJSONObject("receipt").getLong("energy_usage_total") > 150000L);
    Assert.assertEquals(responseContent.getString("contract_address"),shieldAddress);
    Assert.assertEquals(responseContent.getJSONObject("receipt").getString("result"),"SUCCESS");

  }

   /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
  }
}