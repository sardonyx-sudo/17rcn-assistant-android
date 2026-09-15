package org.rotary.goodsassistant.util

import com.google.gson.Gson
import org.rotary.goodsassistant.model.GoodsItem

object GoodsInjector {
    private val gson = Gson()

    fun buildInjectionScript(item: GoodsItem, defaultAddress: String): String {
        val addressToFill = if (!item.address.isNullOrBlank()) item.address else defaultAddress
        val itemJson = gson.toJson(mapOf(
            "title" to (item.title ?: ""),
            "category1" to item.category1Str,
            "category1Name" to (item.category1Name ?: ""),
            "category2" to item.category2Str,
            "category2Name" to (item.category2Name ?: ""),
            "quantity" to item.quantityStr,
            "price" to item.priceStr,
            "condition" to (item.condition ?: "used"),
            "address" to addressToFill,
            "description" to (item.description ?: ""),
            "photos" to item.photos.take(3).map { p ->
                mapOf(
                    "name" to (p.name ?: "goods_${p.fileId ?: "photo"}.jpg"),
                    "mimeType" to (p.mimeType ?: "image/jpeg"),
                    "base64" to (p.base64Data ?: "")
                )
            }
        ))

        return """
        (function() {
            try {
                const data = $itemJson;
                console.log('[RotaryApp] 開始注入物資資料:', data.title);

                function trigger(el, type) {
                    if (!el) return;
                    el.dispatchEvent(new Event(type, { bubbles: true }));
                }

                // 1. 填寫標題 (SR_title)
                const titleInput = document.getElementById('SR_title') || document.querySelector('input[name="SR_title"]');
                if (titleInput && data.title) {
                    titleInput.value = data.title;
                    trigger(titleInput, 'input');
                    trigger(titleInput, 'change');
                }

                // 2. 填寫數量 (SR_quantity)
                const qtyInput = document.getElementById('SR_quantity') || document.querySelector('input[name="SR_quantity"]');
                if (qtyInput) {
                    qtyInput.value = data.quantity || '1';
                    trigger(qtyInput, 'input');
                    trigger(qtyInput, 'change');
                    trigger(qtyInput, 'keyup');
                }

                // 3. 填寫預估單價 (SR_price)
                const priceInput = document.getElementById('SR_price') || document.querySelector('input[name="SR_price"]');
                if (priceInput) {
                    priceInput.value = data.price || '0';
                    trigger(priceInput, 'input');
                    trigger(priceInput, 'change');
                    trigger(priceInput, 'keyup');
                }

                // 4. 物資新舊 (SR_newItem: '1' 為全新, '0' 為二手)
                const condStr = String(data.condition || '').toLowerCase();
                const isNew = (condStr === 'new' || condStr === '1' || condStr.includes('全新'));
                const newRadio = document.querySelector('input[name="SR_newItem"][value="' + (isNew ? '1' : '0') + '"]');
                if (newRadio) {
                    newRadio.checked = true;
                    trigger(newRadio, 'change');
                }

                // 5. 填寫地址 (解析縣市、鄉鎮市區，並透過 jQuery twzipcode 注入，路名填入 SR_addr3)
                const rawAddr = (data.address || '').trim();
                if (rawAddr) {
                    const twDistricts = {
                        '基隆市': ['仁愛區', '信義區', '中正區', '中山區', '安樂區', '暖暖區', '七堵區'],
                        '臺北市': ['中正區', '大同區', '中山區', '松山區', '大安區', '萬華區', '信義區', '士林區', '北投區', '內湖區', '南港區', '文山區'],
                        '新北市': ['萬里區', '金山區', '板橋區', '汐止區', '深坑區', '石碇區', '瑞芳區', '平溪區', '雙溪區', '貢寮區', '新店區', '坪林區', '烏來區', '永和區', '中和區', '土城區', '三峽區', '樹林區', '鶯歌區', '三重區', '新莊區', '泰山區', '林口區', '蘆洲區', '五股區', '八里區', '淡水區', '三芝區', '石門區'],
                        '宜蘭縣': ['宜蘭市', '頭城鎮', '礁溪鄉', '壯圍鄉', '員山鄉', '羅東鎮', '三星鄉', '大同鄉', '五結鄉', '冬山鄉', '蘇澳鎮', '南澳鄉'],
                        '新竹市': ['東區', '北區', '香山區'],
                        '新竹縣': ['竹北市', '湖口鄉', '新豐鄉', '新埔鎮', '關西鎮', '芎林鄉', '寶山鄉', '竹東鎮', '五峰鄉', '橫山鄉', '尖石鄉', '北埔鄉', '峨眉鄉'],
                        '桃園市': ['中壢區', '平鎮區', '龍潭區', '楊梅區', '新屋區', '觀音區', '桃園區', '龜山區', '八德區', '大溪區', '復興區', '大園區', '蘆竹區'],
                        '苗栗縣': ['竹南鎮', '頭份市', '三灣鄉', '南庄鄉', '獅潭鄉', '後龍鎮', '通霄鎮', '苑裡鎮', '苗栗市', '造橋鄉', '頭屋鄉', '公館鄉', '大湖鄉', '泰安鄉', '銅鑼鄉', '三義鄉', '西湖鄉', '卓蘭鎮'],
                        '臺中市': ['中區', '東區', '南區', '西區', '北區', '北屯區', '西屯區', '南屯區', '太平區', '大里區', '霧峰區', '烏日區', '豐原區', '后里區', '石岡區', '東勢區', '和平區', '新社區', '潭子區', '大雅區', '神岡區', '大肚區', '沙鹿區', '龍井區', '梧棲區', '清水區', '大甲區', '外埔區', '大安區'],
                        '彰化縣': ['彰化市', '芬園鄉', '花壇鄉', '秀水鄉', '鹿港鎮', '福興鄉', '線西鄉', '和美鎮', '伸港鄉', '員林市', '社頭鄉', '永靖鄉', '埔心鄉', '溪湖鎮', '大村鄉', '埔鹽鄉', '田中鎮', '北斗鎮', '田尾鄉', '埤頭鄉', '溪州鄉', '竹塘鄉', '二林鎮', '大城鄉', '芳苑鄉', '二水鄉'],
                        '南投縣': ['南投市', '中寮鄉', '草屯鎮', '國姓鄉', '埔里鎮', '仁愛鄉', '名間鄉', '集集鎮', '水里鄉', '魚池鄉', '信義鄉', '竹山鎮', '鹿谷鄉'],
                        '嘉義市': ['東區', '西區'],
                        '嘉義縣': ['番路鄉', '梅山鄉', '竹崎鄉', '阿里山鄉', '中埔鄉', '大埔鄉', '水上鄉', '鹿草鄉', '太保市', '朴子市', '東石鄉', '六腳鄉', '新港鄉', '民雄鄉', '大林鎮', '溪口鄉', '義竹鄉', '布袋鎮'],
                        '雲林縣': ['斗南鎮', '大埤鄉', '虎尾鎮', '土庫鎮', '褒忠鄉', '東勢鄉', '臺西鄉', '崙背鄉', '麥寮鄉', '斗六市', '林內鄉', '古坑鄉', '莿桐鄉', '西螺鎮', '二崙鄉', '北港鎮', '水林鄉', '口湖鄉', '四湖鄉', '元長鄉'],
                        '臺南市': ['中西區', '東區', '南區', '北區', '安平區', '安南區', '永康區', '歸仁區', '新化區', '左鎮區', '玉井區', '楠西區', '南化區', '仁德區', '關廟區', '龍崎區', '官田區', '麻豆區', '佳里區', '西港區', '七股區', '將軍區', '學甲區', '北門區', '新營區', '後壁區', '白河區', '東山區', '六甲區', '下營區', '柳營區', '鹽水區', '善化區', '大內區', '山上區', '新市區', '安定區'],
                        '高雄市': ['新興區', '前金區', '苓雅區', '鹽埕區', '鼓山區', '旗津區', '前鎮區', '三民區', '楠梓區', '小港區', '左營區', '仁武區', '大社區', '岡山區', '路竹區', '阿蓮區', '田寮區', '燕巢區', '橋頭區', '梓官區', '彌陀區', '永安區', '湖內區', '鳳山區', '大寮區', '林園區', '鳥松區', '大樹區', '旗山區', '美濃區', '六龜區', '內門區', '杉林區', '甲仙區', '桃源區', '那瑪夏區', '茂林區', '茄萣區'],
                        '屏東縣': ['屏東市', '三地門鄉', '霧臺鄉', '瑪家鄉', '九如鄉', '里港鄉', '高樹鄉', '鹽埔鄉', '長治鄉', '麟洛鄉', '竹田鄉', '內埔鄉', '萬丹鄉', '潮州鎮', '泰武鄉', '來義鄉', '萬巒鄉', '崁頂鄉', '新埤鄉', '南州鄉', '林邊鄉', '東港鎮', '琉球鄉', '佳冬鄉', '新園鄉', '枋寮鄉', '枋山鄉', '春日鄉', '獅子鄉', '車城鄉', '牡丹鄉', '恆春鎮', '滿州鄉'],
                        '臺東縣': ['臺東市', '綠島鄉', '蘭嶼鄉', '延平鄉', '卑南鄉', '鹿野鄉', '關山鎮', '海端鄉', '池上鄉', '東河鄉', '成功鎮', '長濱鄉', '太麻里鄉', '金峰鄉', '大武鄉', '達仁鄉'],
                        '花蓮縣': ['花蓮市', '新城鄉', '秀林鄉', '吉安鄉', '壽豐鄉', '鳳林鎮', '光復鄉', '豐濱鄉', '瑞穗鄉', '萬榮鄉', '玉里鎮', '卓溪鄉', '富里鄉'],
                        '金門縣': ['金沙鎮', '金湖鎮', '金寧鄉', '金城鎮', '烈嶼鄉', '烏坵鄉'],
                        '連江縣': ['南竿鄉', '北竿鄉', '莒光鄉', '東引鄉'],
                        '澎湖縣': ['馬公市', '西嶼鄉', '望安鄉', '七美鄉', '白沙鄉', '湖西鄉']
                    };

                    let cleanAddr = rawAddr.replace(/^\d{3,5}\s*/, '');
                    let foundCounty = '';
                    let foundDistrict = '';

                    for (const c of Object.keys(twDistricts)) {
                        if (cleanAddr.startsWith(c) || cleanAddr.startsWith(c.replace('臺', '台'))) {
                            foundCounty = c;
                            cleanAddr = cleanAddr.slice(c.length).trim();
                            break;
                        }
                    }

                    if (foundCounty) {
                        const distList = twDistricts[foundCounty] || [];
                        for (const d of distList) {
                            if (cleanAddr.startsWith(d) || cleanAddr.startsWith(d.replace('臺', '台'))) {
                                foundDistrict = d;
                                cleanAddr = cleanAddr.slice(d.length).trim();
                                break;
                            }
                        }
                    }

                    // 尋找 #addr twzipcode 容器
                    const addrBox = document.getElementById('addr');
                    if (addrBox) {
                        const countySel = addrBox.querySelector('select[name^="SR_addr1"]') || addrBox.querySelectorAll('select')[0];
                        if (countySel && foundCounty) {
                            let matchedCountyVal = '';
                            for (let i = 0; i < countySel.options.length; i++) {
                                const opt = countySel.options[i];
                                if (opt.value === foundCounty || opt.text === foundCounty ||
                                    opt.value.replace('臺', '台') === foundCounty.replace('臺', '台') ||
                                    opt.text.replace('臺', '台') === foundCounty.replace('臺', '台')) {
                                    matchedCountyVal = opt.value;
                                    break;
                                }
                            }
                            if (matchedCountyVal) {
                                countySel.value = matchedCountyVal;
                                if (window.$) {
                                    try { $(countySel).val(matchedCountyVal).trigger('change'); } catch(e) {}
                                }
                                trigger(countySel, 'change');
                            }
                        }

                        // 輪詢等待二級鄉鎮區 option 載入完成
                        if (foundDistrict) {
                            let areaAttempts = 0;
                            const areaTimer = setInterval(() => {
                                areaAttempts++;
                                const areaSel = addrBox.querySelector('select[name^="SR_addr2"]') || addrBox.querySelectorAll('select')[1];
                                if (areaSel && areaSel.options.length > 1) {
                                    clearInterval(areaTimer);
                                    let matchedAreaVal = '';
                                    for (let i = 0; i < areaSel.options.length; i++) {
                                        const opt = areaSel.options[i];
                                        if (opt.value === foundDistrict || opt.text === foundDistrict ||
                                            opt.value.replace('臺', '台') === foundDistrict.replace('臺', '台') ||
                                            opt.text.replace('臺', '台') === foundDistrict.replace('臺', '台')) {
                                            matchedAreaVal = opt.value;
                                            break;
                                        }
                                    }
                                    if (matchedAreaVal) {
                                        areaSel.value = matchedAreaVal;
                                        if (window.$) {
                                            try { $(areaSel).val(matchedAreaVal).trigger('change'); } catch(e) {}
                                        }
                                        trigger(areaSel, 'change');
                                    }
                                }
                                if (areaAttempts > 30) clearInterval(areaTimer);
                            }, 150);
                        }

                        // 填寫詳細街道 (SR_addr3)
                        const addr3Input = addrBox.querySelector('input[name="SR_addr3"]') || document.querySelector('input[name="SR_addr3"]');
                        if (addr3Input && cleanAddr) {
                            addr3Input.value = cleanAddr;
                            trigger(addr3Input, 'input');
                            trigger(addr3Input, 'change');
                        }
                    } else {
                        // 備援：若有單一 input[name="SR_address"]
                        const singleAddr = document.getElementById('SR_address') || document.querySelector('input[name="SR_address"]');
                        if (singleAddr) {
                            singleAddr.value = rawAddr;
                            trigger(singleAddr, 'input');
                            trigger(singleAddr, 'change');
                        }
                    }
                }

                // 6. 媒合期限：預設 90 天 (SR_dateRange = '91')
                const dateRadio = document.querySelector('input[name="SR_dateRange"][value="91"]');
                if (dateRadio) {
                    dateRadio.checked = true;
                    trigger(dateRadio, 'change');
                }

                // 7. 建議索取方式：預設自送 (SR_howTake[] = '2')
                const howRadio = document.querySelector('input[name="SR_howTake[]"][value="2"]');
                if (howRadio) {
                    howRadio.checked = true;
                    trigger(howRadio, 'change');
                }

                // 8. 誰能索取：預設任何單位皆可 (SR_onlyCharity = '0')
                const charityRadio = document.querySelector('input[name="SR_onlyCharity"][value="0"]');
                if (charityRadio) {
                    charityRadio.checked = true;
                    trigger(charityRadio, 'change');
                }

                // 9. 一級分類選單匹配 (SR_category1)
                const cat1 = document.getElementById('SR_category1') || document.querySelector('select[name="SR_category1"]');
                if (cat1 && (data.category1 || data.category1Name)) {
                    let matchedVal1 = '';
                    const targetId = String(data.category1 || '').trim();
                    const targetName = String(data.category1Name || '').trim();

                    for (let i = 0; i < cat1.options.length; i++) {
                        const opt = cat1.options[i];
                        if (targetId && opt.value === targetId) {
                            matchedVal1 = opt.value;
                            break;
                        }
                        if (targetName && (opt.text.includes(targetName) || targetName.includes(opt.text))) {
                            matchedVal1 = opt.value;
                            break;
                        }
                    }

                    if (matchedVal1) {
                        cat1.value = matchedVal1;
                        if (window.$) {
                            try { $('#SR_category1').val(matchedVal1).trigger('change'); } catch(e) {}
                        }
                        trigger(cat1, 'change');
                    }
                }

                // 10. 二級分類選單匹配 (SR_category2，輪詢等待 Ajax 載入完成)
                if (data.category2 || data.category2Name) {
                    let attempts = 0;
                    const cat2Timer = setInterval(() => {
                        attempts++;
                        const cat2 = document.getElementById('SR_category2') || document.querySelector('select[name="SR_category2"]');
                        if (cat2 && cat2.options.length > 1) {
                            clearInterval(cat2Timer);
                            let matchedVal2 = '';
                            const targetId2 = String(data.category2 || '').trim();
                            const targetName2 = String(data.category2Name || '').trim();

                            for (let i = 0; i < cat2.options.length; i++) {
                                const opt = cat2.options[i];
                                if (targetId2 && opt.value === targetId2) {
                                    matchedVal2 = opt.value;
                                    break;
                                }
                                if (targetName2 && (opt.text.includes(targetName2) || targetName2.includes(opt.text))) {
                                    matchedVal2 = opt.value;
                                    break;
                                }
                            }

                            if (matchedVal2) {
                                cat2.value = matchedVal2;
                                if (window.$) {
                                    try { $('#SR_category2').val(matchedVal2).trigger('change'); } catch(e) {}
                                }
                                trigger(cat2, 'change');
                            }
                        }
                        if (attempts > 30) clearInterval(cat2Timer);
                    }, 150);
                }

                // 11. KindEditor 富文本內容注入
                if (data.description) {
                    const descLines = data.description.split('\n');
                    const descHtml = descLines.map(l => '<p>' + l.trim() + '</p>').join('');

                    try {
                        if (window.editor && typeof window.editor.html === 'function') {
                            window.editor.html(descHtml);
                            window.editor.sync();
                        }
                    } catch(e) {}

                    try {
                        const keIframe = document.querySelector('.ke-edit-iframe');
                        if (keIframe && keIframe.contentDocument && keIframe.contentDocument.body) {
                            keIframe.contentDocument.body.textContent = '';
                            descLines.forEach(l => {
                                const p = keIframe.contentDocument.createElement('p');
                                p.textContent = l.trim();
                                keIframe.contentDocument.body.appendChild(p);
                            });
                        }
                    } catch(e) {}

                    const textarea = document.getElementById('editor1') || document.querySelector('textarea[name="SR_explain"]');
                    if (textarea) {
                        textarea.value = descHtml;
                        trigger(textarea, 'change');
                    }
                }

                // 12. 照片自動注入 (DataTransfer API)
                if (data.photos && data.photos.length > 0) {
                    const photoInputIds = ['photo1', 'photo2', 'photo3'];
                    data.photos.forEach((photo, idx) => {
                        if (idx >= 3 || !photo.base64) return;
                        const inputEl = document.getElementById(photoInputIds[idx]);
                        if (!inputEl) return;

                        try {
                            const byteChars = atob(photo.base64);
                            const byteNumbers = new Array(byteChars.length);
                            for (let i = 0; i < byteChars.length; i++) {
                                byteNumbers[i] = byteChars.charCodeAt(i);
                            }
                            const byteArray = new Uint8Array(byteNumbers);
                            const blob = new Blob([byteArray], { type: photo.mimeType || 'image/jpeg' });
                            const file = new File([blob], photo.name || ('goods_' + (idx + 1) + '.jpg'), {
                                type: photo.mimeType || 'image/jpeg',
                                lastModified: Date.now()
                            });

                            const dt = new DataTransfer();
                            dt.items.add(file);
                            inputEl.files = dt.files;
                            trigger(inputEl, 'change');
                            console.log('[RotaryApp] 照片 ' + (idx + 1) + ' 注入成功');
                        } catch(err) {
                            console.warn('[RotaryApp] 照片注入失敗:', err);
                        }
                    });
                }

                // 13. 綁定表單送出監聽器向 AndroidBridge 回報
                const form = document.querySelector('form[name="form1"]') || document.querySelector('form');
                if (form && !form.__rotaryBridgeBound) {
                    form.__rotaryBridgeBound = true;
                    form.addEventListener('submit', function() {
                        setTimeout(() => {
                            if (window.AndroidBridge && typeof window.AndroidBridge.onFormSubmitted === 'function') {
                                window.AndroidBridge.onFormSubmitted();
                            }
                        }, 500);
                    });
                }

                console.log('[RotaryApp] 物資注入完成！');
            } catch(e) {
                console.error('[RotaryApp] 注入過程發生錯誤:', e);
            }
        })();
        """.trimIndent()
    }
}
