package com.greyfabric.calculator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "grey_fabric_calculator";
    private static final String RECORDS = "records";
    private static final String RELEASE_CERT_SHA256 = "5D12240CF13B3603D4AAB43366D70F93F1937A60B74C1F1CDC1F35772066F6B5";
    private static final String MEMBER_URL = "http://47.103.114.149/api/member_status.php";
    private static final String REGISTER_URL = "http://47.103.114.149/api/register.php";
    private static final String CALCULATE_URL = "http://47.103.114.149/api/calculate.php";
    private static final String CREATE_ORDER_URL = "http://47.103.114.149/api/create_order.php";
    private static final String CONTACT_TEXT = "客服微信：Cappuccinor123";
    private static final String MEMBER_PHONE = "member_phone";
    private static final String MEMBER_EXPIRE_AT = "member_expire_at";

    private final DecimalFormat priceFormat = new DecimalFormat("0.00");
    private final DecimalFormat weightFormat = new DecimalFormat("0.00");

    private EditText warpDensity;
    private EditText warpCount;
    private EditText warpPrice;
    private EditText warpHemp;
    private EditText warpTencel;
    private EditText weftDensity;
    private EditText weftCount;
    private EditText weftPrice;
    private EditText weftHemp;
    private EditText weftTencel;
    private EditText weft2Density;
    private EditText weft2Count;
    private EditText weft2Price;
    private EditText weft2Hemp;
    private EditText weft2Tencel;
    private EditText greyWidth;
    private EditText finishedWidth;
    private EditText taxFactor;
    private EditText coefficient;
    private EditText plainFee;
    private EditText smallFee;
    private EditText largeFee;
    private EditText shrinkRate;
    private EditText washFee;
    private EditText dyeFee;
    private EditText profitFee;
    private EditText fabricName;
    private EditText recordSearch;
    private EditText memberPhone;
    private EditText memberPassword;
    private LinearLayout calcPage;
    private LinearLayout accountPage;

    private TextView plainTaxedTop;
    private TextView plainUntaxedTop;
    private TextView smallTaxedTop;
    private TextView smallUntaxedTop;
    private TextView largeTaxedTop;
    private TextView largeUntaxedTop;
    private TextView meterWeightView;
    private TextView greyGsmTop;
    private TextView finishedGsmTop;
    private TextView greyGsmView;
    private TextView finishedGsmView;
    private TextView cottonContentView;
    private TextView hempContentView;
    private TextView tencelContentView;
    private TextView saveStatus;
    private TextView memberStatus;
    private LinearLayout savedList;
    private JSONArray recordCache = new JSONArray();
    private LinearLayout infoPage;

    private Result lastPlain = new Result(0, 0);
    private Result lastSmall = new Result(0, 0);
    private Result lastLarge = new Result(0, 0);
    private double lastMeterWeight;
    private double lastGreyGsm;
    private double lastFinishedGsm;
    private double lastCotton;
    private double lastHemp;
    private double lastTencel;
    private boolean memberUsable;
    private boolean hasCloudResult;

    private boolean calculating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.DEBUG && !isReleaseSignatureValid()) {
            showSecurityBlock();
            return;
        }
        setContentView(buildUi());
        loadDefaults();
        renderRecords();
        loadMemberState();
    }

    private boolean isReleaseSignatureValid() {
        try {
            Signature[] signatures;
            PackageManager manager = getPackageManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo info = manager.getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                signatures = info.signingInfo.hasMultipleSigners()
                        ? info.signingInfo.getApkContentsSigners()
                        : info.signingInfo.getSigningCertificateHistory();
            } else {
                PackageInfo info = manager.getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
                signatures = info.signatures;
            }
            if (signatures == null) return false;
            for (Signature signature : signatures) {
                if (RELEASE_CERT_SHA256.equals(sha256(signature.toByteArray()))) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private String sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder builder = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            builder.append(String.format(Locale.US, "%02X", value));
        }
        return builder.toString();
    }

    private void showSecurityBlock() {
        new AlertDialog.Builder(this)
                .setTitle("安全校验失败")
                .setMessage("应用签名异常，请安装官方版本。")
                .setPositiveButton("关闭", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private View buildUi() {
        LinearLayout app = new LinearLayout(this);
        app.setOrientation(LinearLayout.VERTICAL);
        app.setBackgroundColor(Color.rgb(247, 250, 248));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 250, 248));
        app.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), statusBarHeight() + dp(12), dp(12), dp(12));
        scroll.addView(root);

        root.addView(header());

        infoPage = new LinearLayout(this);
        infoPage.setOrientation(LinearLayout.VERTICAL);
        infoPage.setVisibility(View.GONE);
        root.addView(infoPage);
        infoPage.addView(infoPanel());

        calcPage = new LinearLayout(this);
        calcPage.setOrientation(LinearLayout.VERTICAL);
        root.addView(calcPage);
        calcPage.addView(priceBoard());
        calcPage.addView(weightSummary());
        calcPage.addView(contentSummary());
        calcPage.addView(yarnPanel());
        calcPage.addView(widthPanel());
        calcPage.addView(feePanel());
        calcPage.addView(quotePanel());
        calcPage.addView(calculateButton());
        calcPage.addView(savePanel());

        accountPage = new LinearLayout(this);
        accountPage.setOrientation(LinearLayout.VERTICAL);
        accountPage.setVisibility(View.GONE);
        root.addView(accountPage);
        accountPage.addView(memberPanel());
        accountPage.addView(purchasePanel());

        app.addView(bottomNav());
        showPage(1);
        return app;
    }

    private View bottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(6), dp(8), dp(6));
        nav.setBackgroundColor(Color.WHITE);

        Button info = button("说明");
        Button calc = button("计算");
        Button account = button("会员");
        info.setOnClickListener(v -> showPage(0));
        calc.setOnClickListener(v -> showPage(1));
        account.setOnClickListener(v -> showPage(2));
        nav.addView(info, new LinearLayout.LayoutParams(0, dp(52), 1));
        nav.addView(calc, new LinearLayout.LayoutParams(0, dp(52), 1));
        nav.addView(account, new LinearLayout.LayoutParams(0, dp(52), 1));
        return nav;
    }

    private void showPage(int page) {
        if (infoPage != null) infoPage.setVisibility(page == 0 ? View.VISIBLE : View.GONE);
        if (calcPage != null) calcPage.setVisibility(page == 1 ? View.VISIBLE : View.GONE);
        if (accountPage != null) accountPage.setVisibility(page == 2 ? View.VISIBLE : View.GONE);
    }

    private View infoPanel() {
        LinearLayout card = section("软件说明");
        card.addView(text("胚布成本计算器是一款给纺织面料从业者使用的报价工具。", 15, Color.rgb(23, 32, 31), false));
        card.addView(hint("主要功能"));
        card.addView(text("1. 输入经纬密度、经纬支数、门幅、纱价，云端计算胚布价格。", 14, Color.rgb(23, 32, 31), false));
        card.addView(text("2. 同时输出平机、小提花、大提花的含税价和不含税价。", 14, Color.rgb(23, 32, 31), false));
        card.addView(text("3. 自动计算胚布克重、成品克重、棉/麻/天丝含量。", 14, Color.rgb(23, 32, 31), false));
        card.addView(text("4. 支持保存面料记录、模糊搜索历史记录、导出客户报价单。", 14, Color.rgb(23, 32, 31), false));
        card.addView(hint("使用方法"));
        card.addView(text("先到“会员”页面注册或登录账号；新用户有 5 次免费云端计算。", 14, Color.rgb(23, 32, 31), false));
        card.addView(text("回到“计算”页面填写参数，点击“计算确认”后查看结果。", 14, Color.rgb(23, 32, 31), false));
        card.addView(text("需要开通年会员时，到“会员”页面生成订单并联系客服。", 14, Color.rgb(23, 32, 31), false));
        return card;
    }

    private View header() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        header.setPadding(0, 0, 0, dp(8));

        TextView title = text("胚布成本计算器", 24, Color.rgb(23, 32, 31), true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        header.addView(title);

        TextView subtitle = text("报价 · 克重 · 成分", 12, Color.rgb(31, 111, 104), true);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitle.setPadding(0, dp(3), 0, 0);
        header.addView(subtitle);

        return header;
    }

    private View priceBoard() {
        LinearLayout card = card();
        card.setPadding(dp(12), dp(10), dp(12), dp(10));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        card.addView(grid);

        addCell(grid, "机型", true, Color.rgb(99, 112, 109));
        addCell(grid, "含税", true, Color.rgb(99, 112, 109));
        addCell(grid, "不含税", true, Color.rgb(99, 112, 109));

        addCell(grid, "平机", true, Color.rgb(23, 32, 31));
        plainTaxedTop = addCell(grid, "0.00", true, Color.rgb(22, 79, 74));
        plainUntaxedTop = addCell(grid, "0.00", true, Color.rgb(181, 107, 33));

        addCell(grid, "小提花", true, Color.rgb(23, 32, 31));
        smallTaxedTop = addCell(grid, "0.00", true, Color.rgb(22, 79, 74));
        smallUntaxedTop = addCell(grid, "0.00", true, Color.rgb(181, 107, 33));

        addCell(grid, "大提花", true, Color.rgb(23, 32, 31));
        largeTaxedTop = addCell(grid, "0.00", true, Color.rgb(22, 79, 74));
        largeUntaxedTop = addCell(grid, "0.00", true, Color.rgb(181, 107, 33));

        TextView unit = text("单位：元/米", 12, Color.rgb(99, 112, 109), false);
        unit.setPadding(0, dp(6), 0, 0);
        card.addView(unit);
        return card;
    }

    private View weightSummary() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        wrap.setGravity(Gravity.CENTER);
        wrap.setPadding(0, 0, 0, 0);
        wrap.setLayoutParams(panelParams());

        greyGsmTop = summaryBox(wrap, "胚布克重");
        finishedGsmTop = summaryBox(wrap, "成品克重");
        return wrap;
    }

    private TextView summaryBox(LinearLayout parent, String label) {
        LinearLayout box = card();
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(0, 0, dp(8), 0);
        box.setLayoutParams(params);
        box.addView(text(label, 13, Color.rgb(99, 112, 109), false));
        TextView value = text("0.00", 22, Color.rgb(23, 32, 31), true);
        box.addView(value);
        parent.addView(box);
        return value;
    }

    private View yarnPanel() {
        LinearLayout card = section("经纬参数");
        EditText[] warp = yarnRow(card, "经纱");
        warpDensity = warp[0];
        warpCount = warp[1];
        warpPrice = warp[2];
        warpHemp = warp[3];
        warpTencel = warp[4];

        EditText[] weft = yarnRow(card, "纬纱");
        weftDensity = weft[0];
        weftCount = weft[1];
        weftPrice = weft[2];
        weftHemp = weft[3];
        weftTencel = weft[4];

        EditText[] weft2 = yarnRow(card, "纬纱2");
        weft2Density = weft2[0];
        weft2Count = weft2[1];
        weft2Price = weft2[2];
        weft2Hemp = weft2[3];
        weft2Tencel = weft2[4];

        card.addView(hint("纱价填写元/吨，例如 33000；麻/天丝填写百分比，例如 30。"));
        return card;
    }

    private EditText[] yarnRow(LinearLayout parent, String title) {
        TextView rowTitle = text(title, 16, Color.rgb(23, 32, 31), true);
        rowTitle.setPadding(0, dp(8), 0, dp(4));
        parent.addView(rowTitle);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        parent.addView(row);

        EditText density = compactInput(row, "密度");
        EditText count = compactInput(row, "支数");
        EditText price = compactInput(row, "纱价");

        LinearLayout contentRow = new LinearLayout(this);
        contentRow.setOrientation(LinearLayout.HORIZONTAL);
        parent.addView(contentRow);
        EditText hemp = compactInput(contentRow, "麻%");
        EditText tencel = compactInput(contentRow, "天丝%");
        TextView cotton = text("棉%自动计算", 14, Color.rgb(99, 112, 109), true);
        LinearLayout cottonBox = new LinearLayout(this);
        cottonBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        boxParams.setMargins(0, 0, dp(8), 0);
        cottonBox.setLayoutParams(boxParams);
        cottonBox.addView(cotton);
        contentRow.addView(cottonBox);

        return new EditText[] { density, count, price, hemp, tencel };
    }

    private EditText compactInput(LinearLayout row, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        boxParams.setMargins(0, 0, dp(8), 0);
        box.setLayoutParams(boxParams);
        box.addView(text(label, 14, Color.rgb(99, 112, 109), true));
        EditText editText = baseInput();
        box.addView(editText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        ));
        row.addView(box);
        return editText;
    }

    private View widthPanel() {
        LinearLayout card = section("门幅和机型");
        LinearLayout row1 = compactRow(card);
        greyWidth = compactBox(row1, "胚布门幅 英寸");
        finishedWidth = compactBox(row1, "成品门幅 cm");
        LinearLayout row2 = compactRow(card);
        taxFactor = compactBox(row2, "税点系数");
        coefficient = compactBox(row2, "计算系数");
        return card;
    }

    private View feePanel() {
        LinearLayout card = section("加工费");
        LinearLayout feeRow = compactRow(card);
        plainFee = compactBox(feeRow, "平机");
        smallFee = compactBox(feeRow, "小提花");
        largeFee = compactBox(feeRow, "大提花");

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, 0);
        card.addView(row);
        meterWeightView = compactResultBox(row, "总米重", "克/米");
        greyGsmView = compactResultBox(row, "胚布克重", "g/m²");
        finishedGsmView = compactResultBox(row, "成品克重", "g/m²");
        return card;
    }

    private View contentSummary() {
        LinearLayout card = section("整体成分");
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(row);
        cottonContentView = contentBox(row, "棉含量");
        hempContentView = contentBox(row, "麻含量");
        tencelContentView = contentBox(row, "天丝含量");
        return card;
    }

    private TextView contentBox(LinearLayout parent, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(0, 0, dp(8), 0);
        box.setLayoutParams(params);
        box.addView(text(label, 13, Color.rgb(99, 112, 109), false));
        TextView value = text("0.00", 22, Color.rgb(141, 63, 85), true);
        box.addView(value);
        parent.addView(box);
        return value;
    }

    private View memberPanel() {
        LinearLayout card = section("账号登录");
        memberPhone = labeledTextInput(card, "手机号");
        memberPhone.setHint("请输入注册手机号");
        memberPassword = labeledTextInput(card, "密码");
        memberPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        memberPassword.setHint("请输入登录密码");

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button check = button("登录");
        check.setOnClickListener(v -> checkMember(true));
        Button register = button("免费注册");
        register.setOnClickListener(v -> registerAccount());
        Button refresh = button("刷新会员");
        refresh.setOnClickListener(v -> refreshMemberStatus());
        actions.addView(check, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(register, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(refresh, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        card.addView(actions);

        memberStatus = hint("免费注册后赠送 5 次云端计算。试用用完后需要开通会员。");
        card.addView(memberStatus);
        return card;
    }

    private View purchasePanel() {
        LinearLayout card = section("会员购买");
        card.addView(text("年会员：59 元 / 年", 18, Color.rgb(23, 32, 31), true));
        card.addView(hint("当前版本先生成订单，付款后在后台确认收款并自动开通一年会员。"));
        Button order = button("开通年会员 59 元");
        order.setOnClickListener(v -> createYearOrder());
        card.addView(order);
        Button contact = button("联系客服");
        contact.setOnClickListener(v -> showContact());
        card.addView(contact);
        return card;
    }

    private View savePanel() {
        LinearLayout card = section("数据表保存");
        fabricName = labeledTextInput(card, "面料名称");
        Button save = button("保存当前面料");
        save.setOnClickListener(v -> saveCurrent());
        card.addView(save);
        saveStatus = hint("保存后会留在这台手机里，以后可以直接调用。");
        card.addView(saveStatus);
        recordSearch = labeledTextInput(card, "搜索记录");
        recordSearch.setHint("输入面料名称，支持模糊搜索");
        recordSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderRecords(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        savedList = new LinearLayout(this);
        savedList.setOrientation(LinearLayout.VERTICAL);
        card.addView(savedList);
        return card;
    }

    private View quotePanel() {
        LinearLayout card = section("报价单导出");
        LinearLayout row1 = compactRow(card);
        shrinkRate = compactBox(row1, "缩率");
        washFee = compactBox(row1, "水洗费");
        LinearLayout row2 = compactRow(card);
        dyeFee = compactBox(row2, "染费");
        profitFee = compactBox(row2, "利润");
        card.addView(hint("销售价 = 胚布价 / 缩率 + 水洗费 + 染费 + 利润；缩率可填 0.9 或 90。"));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button greyQuote = button("导出胚布报价单");
        greyQuote.setOnClickListener(v -> shareQuote(false));
        Button salesQuote = button("导出销售报价单");
        salesQuote.setOnClickListener(v -> shareQuote(true));
        actions.addView(greyQuote, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(salesQuote, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        card.addView(actions);
        return card;
    }

    private View calculateButton() {
        Button button = button("计算确认");
        button.setTextSize(18);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(31, 111, 104));
        background.setCornerRadius(dp(8));
        button.setBackground(background);
        LinearLayout.LayoutParams params = panelParams();
        params.height = dp(54);
        button.setLayoutParams(params);
        button.setOnClickListener(v -> {
            calculateFromServer(true);
        });
        return button;
    }

    private void loadDefaults() {
        set(warpDensity, 55);
        set(warpCount, 14);
        set(warpPrice, 33000);
        set(warpHemp, 30);
        set(warpTencel, 0);
        set(weftDensity, 48);
        set(weftCount, 16);
        set(weftPrice, 32000);
        set(weftHemp, 30);
        set(weftTencel, 0);
        set(weft2Density, 0);
        set(weft2Count, 16);
        set(weft2Price, 33000);
        set(weft2Hemp, 0);
        set(weft2Tencel, 0);
        set(greyWidth, 108);
        set(finishedWidth, 250);
        set(taxFactor, 0.96);
        set(coefficient, 0.064);
        set(plainFee, 0.03);
        set(smallFee, 0.035);
        set(largeFee, 0.05);
        set(shrinkRate, 0.9);
        set(washFee, 0);
        set(dyeFee, 0);
        set(profitFee, 0);
    }

    private void loadMemberState() {
        if (memberPhone != null) {
            memberPhone.setText(prefs().getString(MEMBER_PHONE, ""));
        }
        memberUsable = false;
        updateMemberStatus();
    }

    private void updateMemberStatus() {
        if (memberStatus == null) return;
        String expireAt = prefs().getString(MEMBER_EXPIRE_AT, "");
        if (memberUsable) {
            if (expireAt.length() > 0 && !"null".equals(expireAt)) {
                memberStatus.setText("会员有效，到期：" + expireAt + "。");
            } else {
                memberStatus.setText("已登录，可使用免费试用额度。");
            }
        } else if (expireAt.length() > 0 && parseServerTime(expireAt) < System.currentTimeMillis()) {
            memberStatus.setText("会员已过期，请续费后重新校验。");
        } else if (expireAt.length() > 0) {
            memberStatus.setText("请登录账号。没有网络时不能使用云端计算。");
        } else {
            memberStatus.setText("免费注册后赠送 5 次云端计算。试用用完后需要开通会员。");
        }
    }

    private void checkMember(boolean showResult) {
        String phone = memberPhone == null ? "" : memberPhone.getText().toString().trim();
        String password = memberPassword == null ? "" : memberPassword.getText().toString();
        if (phone.length() == 0 || password.length() == 0) {
            if (showResult && memberStatus != null) memberStatus.setText("请输入手机号和密码。");
            return;
        }
        prefs().edit().putString(MEMBER_PHONE, phone).apply();
        if (memberStatus != null) memberStatus.setText("正在登录...");

        new Thread(() -> {
            try {
                JSONObject request = new JSONObject();
                request.put("phone", phone);
                request.put("password", password);
                request.put("deviceId", Build.MODEL);
                request.put("appVersion", "1.0.0");
                JSONObject json = postJson(MEMBER_URL, request);
                if (!json.optBoolean("ok", false)) {
                    throw new Exception(json.optString("message", "登录失败"));
                }
                boolean vip = json.optBoolean("vip", false);
                String expireAt = json.optString("expireAt", "");
                int trialRemaining = json.optInt("trialRemaining", 0);
                prefs().edit()
                        .putString(MEMBER_EXPIRE_AT, expireAt)
                        .apply();
                memberUsable = vip || trialRemaining > 0;
                runOnUiThread(() -> {
                    if (memberStatus != null) {
                        if (vip) {
                            memberStatus.setText("会员有效，到期：" + expireAt + "。");
                        } else {
                            memberStatus.setText("登录成功，剩余免费试用 " + trialRemaining + " 次。");
                        }
                    }
                });
            } catch (Exception error) {
                memberUsable = false;
                runOnUiThread(() -> {
                    if (showResult && memberStatus != null) {
                        memberStatus.setText("登录失败：" + error.getMessage());
                    }
                });
            }
        }).start();
    }

    private void registerAccount() {
        String phone = memberPhone == null ? "" : memberPhone.getText().toString().trim();
        String password = memberPassword == null ? "" : memberPassword.getText().toString();
        if (phone.length() == 0 || password.length() == 0) {
            if (memberStatus != null) memberStatus.setText("请输入手机号和密码后再注册。");
            return;
        }
        if (memberStatus != null) memberStatus.setText("正在注册...");
        new Thread(() -> {
            try {
                JSONObject request = new JSONObject();
                request.put("phone", phone);
                request.put("password", password);
                JSONObject json = postJson(REGISTER_URL, request);
                if (!json.optBoolean("ok", false)) {
                    throw new Exception(json.optString("message", "注册失败"));
                }
                prefs().edit().putString(MEMBER_PHONE, phone).apply();
                memberUsable = json.optInt("trialRemaining", 0) > 0;
                runOnUiThread(() -> {
                    if (memberStatus != null) memberStatus.setText("注册成功，已赠送 5 次免费试用。");
                });
            } catch (Exception error) {
                memberUsable = false;
                runOnUiThread(() -> {
                    if (memberStatus != null) memberStatus.setText("注册失败：" + error.getMessage());
                });
            }
        }).start();
    }

    private void refreshMemberStatus() {
        String phone = memberPhone == null ? "" : memberPhone.getText().toString().trim();
        if (phone.length() == 0) {
            if (memberStatus != null) memberStatus.setText("请输入手机号。");
            return;
        }
        String password = memberPassword == null ? "" : memberPassword.getText().toString();
        if (password.length() == 0) {
            if (memberStatus != null) memberStatus.setText("请输入密码。");
            return;
        }
        new Thread(() -> {
            try {
                JSONObject request = new JSONObject();
                request.put("phone", phone);
                request.put("password", password);
                request.put("deviceId", Build.MODEL);
                request.put("appVersion", "1.0.0");
                JSONObject json = postJson(MEMBER_URL, request);
                if (!json.optBoolean("ok", false)) throw new Exception(json.optString("message", "刷新失败"));
                boolean vip = json.optBoolean("vip", false);
                String expireAt = json.optString("expireAt", "");
                int trialRemaining = json.optInt("trialRemaining", 0);
                prefs().edit().putString(MEMBER_EXPIRE_AT, expireAt).apply();
                memberUsable = vip || trialRemaining > 0;
                runOnUiThread(() -> {
                    if (memberStatus != null) {
                        memberStatus.setText(vip ? "会员有效，到期：" + expireAt + "。" : "剩余免费试用 " + trialRemaining + " 次。");
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (memberStatus != null) memberStatus.setText("刷新失败：" + error.getMessage());
                });
            }
        }).start();
    }

    private void createYearOrder() {
        String phone = memberPhone == null ? "" : memberPhone.getText().toString().trim();
        if (phone.length() == 0) {
            if (memberStatus != null) memberStatus.setText("请先输入手机号并登录。");
            return;
        }
        new Thread(() -> {
            try {
                JSONObject request = new JSONObject();
                request.put("phone", phone);
                request.put("planCode", "year_59");
                JSONObject json = postJson(CREATE_ORDER_URL, request);
                if (!json.optBoolean("ok", false)) throw new Exception(json.optString("message", "生成订单失败"));
                String orderNo = json.optString("orderNo");
                String amount = priceFormat.format(json.optDouble("amount", 59));
                runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("订单已生成")
                        .setMessage("订单号：" + orderNo + "\n金额：" + amount + " 元\n请收款后到后台确认开通会员。")
                        .setPositiveButton("知道了", null)
                        .show());
            } catch (Exception error) {
                runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("生成订单失败")
                        .setMessage(error.getMessage())
                        .setPositiveButton("知道了", null)
                        .show());
            }
        }).start();
    }

    private void showContact() {
        copyToClipboard(CONTACT_TEXT);
        new AlertDialog.Builder(this)
                .setTitle("联系客服")
                .setMessage(CONTACT_TEXT + "\n\n已复制到剪贴板。")
                .setPositiveButton("知道了", null)
                .show();
    }

    private long parseServerTime(String value) {
        if (value == null || value.length() == 0 || "null".equals(value)) return 0;
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).parse(value).getTime();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private boolean requireMember(String feature) {
        updateMemberStatus();
        if (memberUsable) return true;
        new AlertDialog.Builder(this)
                .setTitle("需要会员")
                .setMessage(feature + " 需要登录账号；免费试用用完后需要开通会员。")
                .setPositiveButton("去登录", (dialog, which) -> checkMember(true))
                .setNegativeButton("取消", null)
                .show();
        return false;
    }

    private void calculateFromServer(boolean showStatus) {
        if (!requireMember("云端计算")) return;
        String phone = memberPhone == null ? "" : memberPhone.getText().toString().trim();
        if (phone.length() == 0) {
            if (memberStatus != null) memberStatus.setText("请先登录账号后再计算。");
            return;
        }
        String password = memberPassword == null ? "" : memberPassword.getText().toString();
        if (password.length() == 0) {
            if (memberStatus != null) memberStatus.setText("请输入密码并登录后再计算。");
            return;
        }
        if (showStatus && saveStatus != null) saveStatus.setText("正在请求云端计算...");

        new Thread(() -> {
            try {
                JSONObject request = state();
                request.put("phone", phone);
                request.put("password", password);
                request.put("appVersion", "1.0.0");
                JSONObject response = postJson(CALCULATE_URL, request);
                if (!response.optBoolean("ok", false)) {
                    throw new Exception(response.optString("message", "云端计算失败"));
                }
                runOnUiThread(() -> applyCloudResult(response, showStatus));
            } catch (Exception error) {
                hasCloudResult = false;
                runOnUiThread(() -> {
                    if (saveStatus != null) saveStatus.setText("云端计算失败：" + error.getMessage());
                    new AlertDialog.Builder(this)
                            .setTitle("云端计算失败")
                            .setMessage(error.getMessage())
                            .setPositiveButton("知道了", null)
                            .show();
                });
            }
        }).start();
    }

    private JSONObject postJson(String url, JSONObject request) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setDoOutput(true);
        byte[] body = request.toString().getBytes("UTF-8");
        OutputStream output = connection.getOutputStream();
        output.write(body);
        output.close();
        java.io.InputStream stream = connection.getResponseCode() >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();
        if (stream == null) {
            throw new Exception("服务器无响应");
        }
        java.util.Scanner scanner = new java.util.Scanner(stream, "UTF-8").useDelimiter("\\A");
        String response = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return new JSONObject(response);
    }

    private void applyCloudResult(JSONObject response, boolean showStatus) {
        lastPlain = result(response.optJSONObject("plain"));
        lastSmall = result(response.optJSONObject("small"));
        lastLarge = result(response.optJSONObject("large"));
        lastMeterWeight = response.optDouble("meterWeight", 0);
        lastGreyGsm = response.optDouble("greyGsm", 0);
        lastFinishedGsm = response.optDouble("finishedGsm", 0);
        lastCotton = response.optDouble("cotton", 0);
        lastHemp = response.optDouble("hemp", 0);
        lastTencel = response.optDouble("tencel", 0);
        hasCloudResult = true;
        int trialRemaining = response.optInt("trialRemaining", -1);
        boolean vip = response.optBoolean("vip", false);
        if (!vip && trialRemaining >= 0) {
            memberUsable = trialRemaining > 0;
            if (memberStatus != null) memberStatus.setText("免费试用剩余 " + trialRemaining + " 次。");
        }

        plainTaxedTop.setText(priceFormat.format(lastPlain.taxed));
        plainUntaxedTop.setText(priceFormat.format(lastPlain.untaxed));
        smallTaxedTop.setText(priceFormat.format(lastSmall.taxed));
        smallUntaxedTop.setText(priceFormat.format(lastSmall.untaxed));
        largeTaxedTop.setText(priceFormat.format(lastLarge.taxed));
        largeUntaxedTop.setText(priceFormat.format(lastLarge.untaxed));
        meterWeightView.setText(weightFormat.format(lastMeterWeight));
        greyGsmTop.setText(weightFormat.format(lastGreyGsm));
        finishedGsmTop.setText(weightFormat.format(lastFinishedGsm));
        greyGsmView.setText(weightFormat.format(lastGreyGsm));
        finishedGsmView.setText(weightFormat.format(lastFinishedGsm));
        cottonContentView.setText(weightFormat.format(lastCotton));
        hempContentView.setText(weightFormat.format(lastHemp));
        tencelContentView.setText(weightFormat.format(lastTencel));
        if (showStatus && saveStatus != null) saveStatus.setText("云端计算完成。");
    }

    private Result result(JSONObject object) {
        if (object == null) return new Result(0, 0);
        return new Result(object.optDouble("taxed", 0), object.optDouble("untaxed", 0));
    }

    private void shareQuote(boolean sales) {
        if (!requireMember("导出报价单")) return;
        if (!hasCloudResult) {
            new AlertDialog.Builder(this)
                    .setTitle("请先计算")
                    .setMessage("请先点击“计算确认”，由服务器计算完成后再导出报价单。")
                    .setPositiveButton("去计算", (dialog, which) -> calculateFromServer(true))
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }
        String name = fabricName == null ? "" : fabricName.getText().toString().trim();
        if (name.length() == 0) name = "未命名面料";
        final String quoteName = name;
        String[] machines = new String[] {"平机", "小提花", "大提花"};
        new AlertDialog.Builder(this)
                .setTitle("选择织布机型")
                .setItems(machines, (dialog, which) -> exportSelectedQuote(sales, quoteName, which))
                .show();
    }

    private void exportSelectedQuote(boolean sales, String name, int machineIndex) {
        String machineName = machineName(machineIndex);
        double price = machinePrice(machineIndex, sales);
        String title = sales ? "销售报价单" : "胚布报价单";
        String text = customerQuoteText(title, name, machineName, price);
        copyToClipboard(text);
        showQuoteDialog(title, text);
    }

    private void showQuoteDialog(String title, String text) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(text)
                .setNegativeButton("关闭", null)
                .setPositiveButton("系统分享", (dialog, which) -> shareText(title, text))
                .show();
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("报价单", text));
        }
    }

    private void shareText(String title, String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        try {
            startActivity(Intent.createChooser(intent, "导出报价单"));
        } catch (ActivityNotFoundException error) {
            new AlertDialog.Builder(this)
                    .setTitle("已复制")
                    .setMessage("没有找到可分享的应用，报价单已复制到剪贴板。")
                    .setPositiveButton("知道了", null)
                    .show();
        }
    }

    private String customerQuoteText(String title, String name, String machineName, double price) {
        return "【" + title + "】\n"
                + "面料名称：" + name + "\n"
                + "织造方式：" + machineName + "\n"
                + "门幅：" + weightFormat.format(value(finishedWidth)) + " cm\n"
                + "克重：" + weightFormat.format(lastFinishedGsm) + " g/m²\n"
                + "成分：" + quoteCompositionText() + "\n"
                + "价格：" + priceFormat.format(price) + " 元/米";
    }

    private String quoteCompositionText() {
        StringBuilder builder = new StringBuilder();
        appendCompositionPart(builder, "棉", lastCotton);
        appendCompositionPart(builder, "麻", lastHemp);
        appendCompositionPart(builder, "天丝", lastTencel);
        return builder.toString();
    }

    private void appendCompositionPart(StringBuilder builder, String name, double percent) {
        if (percent <= 0.005) return;
        if (builder.length() > 0) builder.append(" / ");
        builder.append(name).append(" ").append(weightFormat.format(percent)).append("%");
    }

    private double machinePrice(int machineIndex, boolean sales) {
        double greyPrice;
        if (machineIndex == 1) {
            greyPrice = lastSmall.taxed;
        } else if (machineIndex == 2) {
            greyPrice = lastLarge.taxed;
        } else {
            greyPrice = lastPlain.taxed;
        }
        if (!sales) return greyPrice;
        double shrink = normalizedShrink();
        double wash = value(washFee);
        double dye = value(dyeFee);
        double profit = value(profitFee);
        return salesPrice(greyPrice, shrink, wash, dye, profit);
    }

    private String machineName(int machineIndex) {
        if (machineIndex == 1) return "小提花";
        if (machineIndex == 2) return "大提花";
        return "平机";
    }

    private double normalizedShrink() {
        double shrink = value(shrinkRate);
        if (shrink > 1) shrink = shrink / 100;
        return shrink > 0 ? shrink : 1;
    }

    private double salesPrice(double greyPrice, double shrink, double wash, double dye, double profit) {
        return greyPrice / shrink + wash + dye + profit;
    }

    private void saveCurrent() {
        if (!requireMember("保存数据表")) return;
        String name = fabricName.getText().toString().trim();
        if (name.length() == 0) {
            saveStatus.setText("先给这个面料起个名字，再保存。");
            return;
        }
        try {
            JSONArray records = records();
            JSONObject item = new JSONObject();
            item.put("name", name);
            item.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date()));
            item.put("state", state());

            JSONArray next = new JSONArray();
            next.put(item);
            for (int i = 0; i < records.length(); i++) {
                JSONObject old = records.getJSONObject(i);
                if (!name.equals(old.optString("name"))) next.put(old);
            }
            prefs().edit().putString(RECORDS, next.toString()).apply();
            saveStatus.setText("已保存：" + name);
            renderRecords();
        } catch (Exception error) {
            saveStatus.setText("保存失败：" + error.getMessage());
        }
    }

    private void renderRecords() {
        savedList.removeAllViews();
        try {
            recordCache = records();
            String query = recordSearch == null ? "" : recordSearch.getText().toString().trim();
            int shown = 0;
            if (recordCache.length() == 0) {
                savedList.addView(hint("还没有保存过的面料。"));
                return;
            }
            for (int i = 0; i < recordCache.length(); i++) {
                JSONObject record = recordCache.getJSONObject(i);
                if (!matchesRecord(record.optString("name"), query)) continue;
                savedList.addView(recordRow(record));
                shown++;
            }
            if (shown == 0) {
                savedList.addView(hint("没有找到匹配的面料。"));
            }
        } catch (Exception error) {
            savedList.addView(hint("读取保存数据失败。"));
        }
    }

    private boolean matchesRecord(String name, String query) {
        if (query.length() == 0) return true;
        String source = name.toLowerCase(Locale.CHINA);
        String target = query.toLowerCase(Locale.CHINA);
        if (source.contains(target)) return true;

        int index = 0;
        for (int i = 0; i < source.length() && index < target.length(); i++) {
            if (source.charAt(i) == target.charAt(index)) index++;
        }
        return index == target.length();
    }

    private View recordRow(JSONObject record) throws Exception {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(10), 0, 0);

        TextView name = text(record.optString("name"), 16, Color.rgb(23, 32, 31), true);
        row.addView(name);
        row.addView(hint(record.optString("date")));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button load = button("调用");
        load.setOnClickListener(v -> {
            try {
                applyState(record.getJSONObject("state"));
                fabricName.setText(record.optString("name"));
                saveStatus.setText("已调用：" + record.optString("name"));
            } catch (Exception error) {
                saveStatus.setText("调用失败：" + error.getMessage());
            }
        });
        Button delete = button("删除");
        delete.setOnClickListener(v -> confirmDelete(record.optString("name")));
        actions.addView(load, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(delete, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(actions);
        return row;
    }

    private void confirmDelete(String name) {
        new AlertDialog.Builder(this)
                .setTitle("删除面料")
                .setMessage("删除“" + name + "”？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> deleteRecord(name))
                .show();
    }

    private void deleteRecord(String name) {
        try {
            JSONArray old = records();
            JSONArray next = new JSONArray();
            for (int i = 0; i < old.length(); i++) {
                JSONObject item = old.getJSONObject(i);
                if (!name.equals(item.optString("name"))) next.put(item);
            }
            prefs().edit().putString(RECORDS, next.toString()).apply();
            saveStatus.setText("已删除：" + name);
            renderRecords();
        } catch (Exception error) {
            saveStatus.setText("删除失败：" + error.getMessage());
        }
    }

    private JSONObject state() throws Exception {
        JSONObject state = new JSONObject();
        state.put("warpDensity", value(warpDensity));
        state.put("warpCount", value(warpCount));
        state.put("warpPrice", value(warpPrice));
        state.put("warpHemp", value(warpHemp));
        state.put("warpTencel", value(warpTencel));
        state.put("weftDensity", value(weftDensity));
        state.put("weftCount", value(weftCount));
        state.put("weftPrice", value(weftPrice));
        state.put("weftHemp", value(weftHemp));
        state.put("weftTencel", value(weftTencel));
        state.put("weft2Density", value(weft2Density));
        state.put("weft2Count", value(weft2Count));
        state.put("weft2Price", value(weft2Price));
        state.put("weft2Hemp", value(weft2Hemp));
        state.put("weft2Tencel", value(weft2Tencel));
        state.put("greyWidth", value(greyWidth));
        state.put("finishedWidth", value(finishedWidth));
        state.put("taxFactor", value(taxFactor));
        state.put("coefficient", value(coefficient));
        state.put("plainFee", value(plainFee));
        state.put("smallFee", value(smallFee));
        state.put("largeFee", value(largeFee));
        state.put("shrinkRate", value(shrinkRate));
        state.put("washFee", value(washFee));
        state.put("dyeFee", value(dyeFee));
        state.put("profitFee", value(profitFee));
        return state;
    }

    private void applyState(JSONObject state) {
        set(warpDensity, state.optDouble("warpDensity", 55));
        set(warpCount, state.optDouble("warpCount", 14));
        set(warpPrice, state.optDouble("warpPrice", 33000));
        set(warpHemp, state.optDouble("warpHemp", 30));
        set(warpTencel, state.optDouble("warpTencel", 0));
        set(weftDensity, state.optDouble("weftDensity", 48));
        set(weftCount, state.optDouble("weftCount", 16));
        set(weftPrice, state.optDouble("weftPrice", 32000));
        set(weftHemp, state.optDouble("weftHemp", 30));
        set(weftTencel, state.optDouble("weftTencel", 0));
        set(weft2Density, state.optDouble("weft2Density", 0));
        set(weft2Count, state.optDouble("weft2Count", 16));
        set(weft2Price, state.optDouble("weft2Price", 33000));
        set(weft2Hemp, state.optDouble("weft2Hemp", 0));
        set(weft2Tencel, state.optDouble("weft2Tencel", 0));
        set(greyWidth, state.optDouble("greyWidth", 108));
        set(finishedWidth, state.optDouble("finishedWidth", 250));
        set(taxFactor, state.optDouble("taxFactor", 0.96));
        set(coefficient, state.optDouble("coefficient", 0.064));
        set(plainFee, state.optDouble("plainFee", 0.03));
        set(smallFee, state.optDouble("smallFee", 0.035));
        set(largeFee, state.optDouble("largeFee", 0.05));
        set(shrinkRate, state.optDouble("shrinkRate", 0.9));
        set(washFee, state.optDouble("washFee", 0));
        set(dyeFee, state.optDouble("dyeFee", 0));
        set(profitFee, state.optDouble("profitFee", 0));
        hasCloudResult = false;
        if (saveStatus != null) saveStatus.setText("已调用保存参数，请重新点击计算确认。");
    }

    private JSONArray records() throws Exception {
        return new JSONArray(prefs().getString(RECORDS, "[]"));
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private TextView addCell(GridLayout grid, String value, boolean bold, int color) {
        TextView view = text(value, 15, color, bold);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = getResources().getDisplayMetrics().widthPixels / 3 - dp(22);
        params.setMargins(0, dp(5), 0, dp(5));
        view.setLayoutParams(params);
        grid.addView(view);
        return view;
    }

    private EditText input(GridLayout grid) {
        EditText editText = baseInput();
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = getResources().getDisplayMetrics().widthPixels / 4 - dp(16);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        editText.setLayoutParams(params);
        grid.addView(editText);
        return editText;
    }

    private EditText labeledInput(LinearLayout parent, String label) {
        parent.addView(text(label, 13, Color.rgb(99, 112, 109), false));
        EditText editText = baseInput();
        parent.addView(editText);
        return editText;
    }

    private EditText compactLabeledInput(LinearLayout parent, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));
        parent.addView(row);

        TextView labelView = text(label, 14, Color.rgb(99, 112, 109), false);
        row.addView(labelView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        EditText editText = baseInput();
        row.addView(editText, new LinearLayout.LayoutParams(0, dp(42), 1));
        return editText;
    }

    private LinearLayout compactRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(2), 0, dp(2));
        parent.addView(row);
        return row;
    }

    private EditText compactBox(LinearLayout row, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(0, 0, dp(8), 0);
        row.addView(box, params);
        box.addView(text(label, 14, Color.rgb(99, 112, 109), true));
        EditText editText = baseInput();
        box.addView(editText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
        ));
        return editText;
    }

    private EditText labeledTextInput(LinearLayout parent, String label) {
        parent.addView(text(label, 14, Color.rgb(99, 112, 109), true));
        EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setTextSize(20);
        editText.setTextColor(Color.rgb(23, 32, 31));
        editText.setBackground(inputBackground());
        editText.setPadding(dp(10), 0, dp(10), 0);
        parent.addView(editText);
        return editText;
    }

    private EditText baseInput() {
        EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setTextSize(18);
        editText.setTextColor(Color.rgb(23, 32, 31));
        editText.setBackground(inputBackground());
        editText.setPadding(dp(10), 0, dp(10), 0);
        editText.setSelectAllOnFocus(true);
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {}
        });
        return editText;
    }

    private GradientDrawable inputBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(dp(7));
        drawable.setStroke(dp(1), Color.rgb(185, 205, 201));
        return drawable;
    }

    private TextView resultLine(LinearLayout parent, String label, String unit) {
        TextView value = text("0.00", 22, Color.rgb(141, 63, 85), true);
        TextView labelView = text(label + " / " + unit, 13, Color.rgb(99, 112, 109), false);
        labelView.setPadding(0, dp(12), 0, 0);
        parent.addView(labelView);
        parent.addView(value);
        return value;
    }

    private TextView compactResultBox(LinearLayout parent, String label, String unit) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(0, 0, dp(8), 0);
        parent.addView(box, params);
        box.addView(text(label, 12, Color.rgb(99, 112, 109), false));
        TextView value = text("0.00", 20, Color.rgb(141, 63, 85), true);
        box.addView(value);
        box.addView(text(unit, 11, Color.rgb(99, 112, 109), false));
        return value;
    }

    private LinearLayout section(String title) {
        LinearLayout card = card();
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView heading = text(title, 18, Color.rgb(23, 32, 31), true);
        heading.setPadding(0, 0, 0, dp(8));
        card.addView(heading);
        return card;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setLayoutParams(panelParams());
        return card;
    }

    private LinearLayout.LayoutParams panelParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(8), 0, dp(4));
        return params;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(15);
        return button;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView hint(String value) {
        TextView view = text(value, 12, Color.rgb(99, 112, 109), false);
        view.setPadding(0, dp(8), 0, 0);
        return view;
    }

    private void set(EditText editText, double value) {
        editText.setText(value == (long) value ? String.valueOf((long) value) : String.valueOf(value));
    }

    private double value(EditText editText) {
        try {
            String text = editText.getText().toString().trim();
            return text.length() == 0 ? 0 : Double.parseDouble(text);
        } catch (Exception error) {
            return 0;
        }
    }

    private int statusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class Result {
        final double taxed;
        final double untaxed;

        Result(double taxed, double untaxed) {
            this.taxed = taxed;
            this.untaxed = untaxed;
        }
    }
}
