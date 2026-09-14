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

                // 5. 填寫地址 (SR_address)
                const addrInput = document.getElementById('SR_address') || document.querySelector('input[name="SR_address"]');
                if (addrInput && data.address) {
                    addrInput.value = data.address;
                    trigger(addrInput, 'input');
                    trigger(addrInput, 'change');
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
