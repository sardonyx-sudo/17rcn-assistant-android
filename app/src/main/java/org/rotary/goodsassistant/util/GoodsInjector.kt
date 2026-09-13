package org.rotary.goodsassistant.util

import com.google.gson.Gson
import org.rotary.goodsassistant.model.GoodsItem

object GoodsInjector {
    private val gson = Gson()

    fun buildInjectionScript(item: GoodsItem, defaultAddress: String): String {
        val addressToFill = if (!item.address.isNullOrBlank()) item.address else defaultAddress
        val itemJson = gson.toJson(mapOf(
            "title" to (item.title ?: ""),
            "category1" to (item.category1 ?: ""),
            "category2" to (item.category2 ?: ""),
            "quantity" to (item.quantity ?: "1"),
            "condition" to (item.condition ?: "良好"),
            "address" to addressToFill,
            "description" to (item.description ?: ""),
            "photos" to item.photos.map { p ->
                mapOf(
                    "name" to (p.name ?: "photo.jpg"),
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

                // 1. 填寫標題
                const titleInput = document.getElementById('SR_title') || document.querySelector('input[name="SR_title"]');
                if (titleInput && data.title) {
                    titleInput.value = data.title;
                    trigger(titleInput, 'input');
                    trigger(titleInput, 'change');
                }

                // 2. 填寫數量
                const amountInput = document.getElementById('SR_amount') || document.querySelector('input[name="SR_amount"]');
                if (amountInput) {
                    amountInput.value = data.quantity || '1';
                    trigger(amountInput, 'input');
                    trigger(amountInput, 'change');
                }

                // 3. 填寫地址
                const addrInput = document.getElementById('SR_address') || document.querySelector('input[name="SR_address"]');
                if (addrInput && data.address) {
                    addrInput.value = data.address;
                    trigger(addrInput, 'input');
                    trigger(addrInput, 'change');
                }

                // 4. 新舊程度單選框
                const condition = data.condition || '良好';
                const radios = document.querySelectorAll('input[name="SR_old"]');
                radios.forEach(r => {
                    const label = r.closest('label') ? r.closest('label').textContent : '';
                    if (label.includes(condition) || r.value.includes(condition)) {
                        r.checked = true;
                        trigger(r, 'change');
                    }
                });

                // 5. 分類選單匹配 (第 1 層)
                const cat1 = document.getElementById('SR_category1') || document.querySelector('select[name="SR_category1"]');
                if (cat1 && data.category1) {
                    let matchedVal1 = '';
                    for (let i = 0; i < cat1.options.length; i++) {
                        const opt = cat1.options[i];
                        if (opt.text.includes(data.category1) || data.category1.includes(opt.text)) {
                            matchedVal1 = opt.value;
                            break;
                        }
                    }
                    if (matchedVal1) {
                        cat1.value = matchedVal1;
                        trigger(cat1, 'change');
                    }
                }

                // 6. 分類選單匹配 (第 2 層，輪詢等待 Ajax 載入)
                if (data.category2) {
                    let attempts = 0;
                    const catTimer = setInterval(() => {
                        attempts++;
                        const cat2 = document.getElementById('SR_category2') || document.querySelector('select[name="SR_category2"]');
                        if (cat2 && cat2.options.length > 1) {
                            clearInterval(catTimer);
                            let matchedVal2 = '';
                            for (let i = 0; i < cat2.options.length; i++) {
                                const opt = cat2.options[i];
                                if (opt.text.includes(data.category2) || data.category2.includes(opt.text)) {
                                    matchedVal2 = opt.value;
                                    break;
                                }
                            }
                            if (matchedVal2) {
                                cat2.value = matchedVal2;
                                trigger(cat2, 'change');
                            }
                        }
                        if (attempts > 30) clearInterval(catTimer);
                    }, 150);
                }

                // 7. KindEditor 富文本內容注入
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

                // 8. 注入照片檔案 (DataTransfer API)
                if (data.photos && data.photos.length > 0) {
                    data.photos.forEach((photo, idx) => {
                        if (!photo.base64) return;
                        const inputName = 'SR_file' + (idx + 1);
                        const fileInput = document.querySelector('input[name="' + inputName + '"]');
                        if (!fileInput) return;

                        try {
                            const byteChars = atob(photo.base64);
                            const byteNumbers = new Array(byteChars.length);
                            for (let i = 0; i < byteChars.length; i++) {
                                byteNumbers[i] = byteChars.charCodeAt(i);
                            }
                            const byteArray = new Uint8Array(byteNumbers);
                            const blob = new Blob([byteArray], { type: photo.mimeType || 'image/jpeg' });
                            const file = new File([blob], photo.name || ('photo_' + (idx + 1) + '.jpg'), { type: blob.type });

                            const dt = new DataTransfer();
                            dt.items.add(file);
                            fileInput.files = dt.files;
                            trigger(fileInput, 'change');
                        } catch(err) {
                            console.warn('[RotaryApp] 照片注入失敗:', err);
                        }
                    });
                }

                // 9. 監聽送出按鈕 (通知 Android 刊登成功)
                const form = document.querySelector('form[action*="goods"]') || document.querySelector('form');
                if (form && !form.dataset.assistantHooked) {
                    form.dataset.assistantHooked = 'true';
                    form.addEventListener('submit', function() {
                        if (window.AndroidBridge && typeof window.AndroidBridge.onFormSubmitted === 'function') {
                            window.AndroidBridge.onFormSubmitted();
                        }
                    });
                }

                // 將畫面自動滾動至圖形驗證碼區域，方便人員輸入
                const captchaImg = document.querySelector('img[src*="code"]') || document.querySelector('input[name="code"]') || document.querySelector('input[name="chk_code"]');
                if (captchaImg) {
                    captchaImg.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    const captchaInput = document.querySelector('input[name="code"]') || document.querySelector('input[name="chk_code"]');
                    if (captchaInput) captchaInput.focus();
                }

                return 'SUCCESS';
            } catch(e) {
                console.error('[RotaryApp] 注入失敗:', e);
                return 'ERROR: ' + e.message;
            }
        })();
        """.trimIndent()
    }
}
