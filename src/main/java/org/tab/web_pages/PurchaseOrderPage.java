package org.tab.web_pages;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.NoSuchElementException;

import static org.tab.utils.common.SharedMethods.*;

public class PurchaseOrderPage {

    @FindBy (xpath="//a[contains(@href, \"/dashboard/purchase-receives/create\")]")
    public WebElement createNewPurchaseOrderBtn;
    @FindBy (id="select2-supplier_id-container")
    public WebElement supplierDDLContainer;
    @FindBy (xpath="//ul[@id='select2-supplier_id-results']//li")
    public List<WebElement> supplierDDLOptions;
    @FindBy (id="order_discount")
    public WebElement discountInput;
    @FindBy (xpath="(//input[contains(@placeholder,'XXX')])[2]")
    public WebElement barcodeInput;
    @FindBy (xpath="//div[@id='options-container']//tbody//tr[2]//td[2]")
    public WebElement productDDLContainer;
    @FindBy (xpath="//div[@id='options-container']//tbody//tr[2]//td[2]//select")
    public WebElement productDDL;
    @FindBy (xpath="//div[@id='options-container']//tbody//tr[2]//td[3]//input")
    public WebElement quantityInput;
    @FindBy (xpath="//div[@id='options-container']//tbody//tr[2]//td[4]//input")
    public WebElement bonusQuantityInput;
    @FindBy (xpath="//div[@id='options-container']//tbody//tr[2]//td[7]//select")
    public WebElement discountTypeDDL;
    @FindBy (xpath="//div[@id='options-container']//tbody//tr[2]//td[7]//input")
    public WebElement itemDiscountInput;
    @FindBy (xpath="//div[@id='options-container']//tbody//tr[2]//td[10]//button")
    public WebElement lotIcon;
    @FindBy (xpath="//select[@name='lot_id[]']")
    public WebElement lotDDL;
    @FindBy (xpath="//button[normalize-space()='Confirm']")
    public WebElement lotConfirmBtn;
    @FindBy (xpath="//input[@name='create_new_lot[]']")
    public WebElement createNewLotCheckbox;
    @FindBy (xpath="//label[normalize-space()='Create New LOT']")
    public WebElement createNewLotLabel;
    @FindBy (xpath="//input[@name='lot_number[]']")
    public WebElement lotNumber;
    @FindBy (xpath="//select[@name='day[]']")
    public WebElement lotDay;
    @FindBy (xpath="//select[@name='month[]']")
    public WebElement lotMonth;
    @FindBy (xpath="//select[@name='year[]']")
    public WebElement lotYear;
    @FindBy (id="update_purchase_order")
    public WebElement confirmPurchaseBtn;
    @FindBy (id="saveAsDraft")
    public WebElement saveAsDraftBtn;


    public PurchaseOrderPage(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }

    public void selectDDL(WebElement locator, String value, int index){
        /*locator.click();*/
        if (value.isEmpty()){
            Select ddl = new Select(locator);
            ddl.selectByIndex(index);
        } else
            locator.sendKeys(value, Keys.ENTER);
    }

    public void fillPurchaseOrderForm(
            String barcode,
            WebElement productDDL,
            String productName,
            String quantity,
            String bonusQuantity,
            WebElement discountTypeDDL,
            String discountTypeValue,
            String itemDiscount,
            WebElement lotDDL,
            int lotIndex
    ) {
        if (barcode.isEmpty()){
            productDDLContainer.click();
            staticWait(800);
            selectDDL(productDDL, productName, 1);
        }else {
            staticWait(200);
            barcodeInput.sendKeys(barcode, Keys.ENTER);
        }
        waitUntilElementClickable(quantityInput);
        quantityInput.sendKeys(quantity);
        waitUntilElementClickable(bonusQuantityInput);
        bonusQuantityInput.clear();
        bonusQuantityInput.sendKeys(bonusQuantity);
        if (discountTypeValue.isEmpty())
            selectDDL(discountTypeDDL, "", 1);
        else
            selectDDL(discountTypeDDL, discountTypeValue, 0);
        itemDiscountInput.sendKeys(itemDiscount);

        try {
            waitUntilElementClickable(lotIcon);
            lotIcon.click();
            waitUntilElementClickable(lotDDL);
            lotDDL.click();
            try {
                selectDDL(lotDDL, "", lotIndex);
            }catch (Exception ex){
                createNewLot();
            }
            lotConfirmBtn.click();
        } catch (Exception e){
            System.out.print("❌ No lot available for this product. ");
        }
    }

    public void ulList(List<WebElement> locator){
        if (locator.isEmpty()) {
            throw new NoSuchElementException("❌ No Edit links found in products table.");
        }
        int id= generateRandomNumber(locator.size()) + 1;
        if (id>= locator.size())
            id=id-1;
        locator.get(id).click();
    }

    public void createNewLot(){
        createNewLotLabel.click();
        try {
            lotNumber.sendKeys("541318");

        } catch (Exception ignored) {}
        selectDDL(lotDay, "15", 0);
        selectDDL(lotMonth, "08", 0);
        selectDDL(lotYear, "2027", 0);
    }
}
