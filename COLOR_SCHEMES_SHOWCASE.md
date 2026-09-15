# 🎨 扶輪公益網 Android App 視覺配色示範指南

本文件針對「**淡米色主色調**」提供兩種風格的視覺配色示範與色票代碼，並透過色塊與模擬卡片展示在 App 頂部列、標籤列、物資卡片與操作按鈕上的實體呈現效果。

---

## 📊 方案快速比對

| 比較項目 | 🌟 方案 A：日系溫暖焦糖米 (推薦) | 🍵 方案 B：極簡奶霜燕麥米 |
| :--- | :--- | :--- |
| **視覺意象** | 無印日系、溫暖厚實、質感沉穩 | 莫蘭迪低飽和、輕盈柔和、極簡文青 |
| **頂部導航/狀態列** | 暖砂米褐 `#EDE6DB` | 淺燕麥米 `#F4EFEA` |
| **主要行動按鈕 (CTA)** | **焦糖暖褐 `#8C6239`**（配白字） | **深奶茶米 `#D0C4B4`**（配深褐字） |
| **全域背景底色** | 暖白米 `#FAF7F2` | 淺米灰 `#FBF9F6` |
| **強光/戶外可讀性** | ⭐⭐⭐⭐⭐（按鈕對比極佳，大太陽下清楚可見） | ⭐⭐⭐⭐☆（柔和高雅，對比度較為內斂） |

---

## 🌟 方案 A：日系溫暖焦糖米 (Warm Caramel & Sand Beige)

> **設計核心**：全體以舒適的「淡米色」為大面積基底，但關鍵操作按鈕採用沉穩濃郁的「焦糖暖褐色」，兼具淡米色的溫暖護眼，又擁有高清晰度的點擊引導。

### 1. 色票調色盤 (Color Palette)

<table>
  <tr>
    <th width="30%">角色名稱</th>
    <th width="25%">色碼 (Hex)</th>
    <th width="45%">顏色實體預覽</th>
  </tr>
  <tr>
    <td><b>全域底色 (Background)</b></td>
    <td><code>#FAF7F2</code></td>
    <td style="background-color: #FAF7F2; border: 1px solid #E2DBD0; padding: 12px; color: #2D2620; font-weight: bold;">暖米白底色 #FAF7F2</td>
  </tr>
  <tr>
    <td><b>頂部標題列 (AppBar)</b></td>
    <td><code>#EDE6DB</code></td>
    <td style="background-color: #EDE6DB; padding: 12px; color: #2D2620; font-weight: bold;">暖砂米褐 #EDE6DB</td>
  </tr>
  <tr>
    <td><b>主操作按鈕 (Primary CTA)</b></td>
    <td><code>#8C6239</code></td>
    <td style="background-color: #8C6239; padding: 12px; color: #FFFFFF; font-weight: bold; border-radius: 6px;">焦糖暖褐（填入/刊登按鈕）#8C6239</td>
  </tr>
  <tr>
    <td><b>卡片狀態標籤 (Badge)</b></td>
    <td><code>#F2EBD9</code></td>
    <td style="background-color: #F2EBD9; padding: 12px; color: #6E4E2D; font-weight: bold; border-radius: 4px;">待刊登標籤 #F2EBD9 (文字 #6E4E2D)</td>
  </tr>
  <tr>
    <td><b>主要文字 (Text Primary)</b></td>
    <td><code>#2D2620</code></td>
    <td style="background-color: #FFFFFF; padding: 12px; color: #2D2620; font-weight: bold;">深焙褐黑標題字 #2D2620</td>
  </tr>
  <tr>
    <td><b>卡片邊框與分隔線</b></td>
    <td><code>#E2DBD0</code></td>
    <td style="background-color: #E2DBD0; padding: 12px; color: #5C5248;">細緻米灰邊框 #E2DBD0</td>
  </tr>
</table>

### 2. UI 介面模擬預覽（方案 A）

<div style="background-color: #FAF7F2; padding: 16px; border-radius: 12px; border: 1px solid #E2DBD0; max-width: 420px; font-family: sans-serif;">

  <!-- 頂部標題列 -->
  <div style="background-color: #EDE6DB; padding: 12px 16px; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #E2DBD0;">
    <span style="color: #2D2620; font-weight: bold; font-size: 15px;">扶輪公益網物資助手</span>
    <span style="background-color: #DFD5C6; color: #4A3E35; font-size: 12px; padding: 4px 10px; border-radius: 12px;">🔄 刷新</span>
  </div>

  <!-- 分頁列 -->
  <div style="display: flex; justify-content: space-around; padding: 10px 0; border-bottom: 2px solid #EDE6DB; margin-bottom: 12px;">
    <span style="color: #8C6239; font-weight: bold; border-bottom: 2px solid #8C6239; padding-bottom: 4px;">物資佇列</span>
    <span style="color: #7A7067;">17rcn 刊登</span>
    <span style="color: #7A7067;">偏好設定</span>
  </div>

  <!-- 物資卡片模擬 -->
  <div style="background-color: #FFFFFF; padding: 14px; border-radius: 8px; border: 1px solid #E2DBD0; box-shadow: 0 2px 4px rgba(0,0,0,0.03); margin-bottom: 12px;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
      <span style="background-color: #F2EBD9; color: #6E4E2D; font-size: 11px; padding: 3px 8px; border-radius: 4px; font-weight: bold;">待刊登</span>
      <span style="color: #8A7F75; font-size: 12px;">電腦周邊 · 數量：1</span>
    </div>
    <div style="color: #2D2620; font-weight: bold; font-size: 15px; margin-bottom: 6px;">羅技 M650 靜音無線藍牙滑鼠</div>
    <div style="color: #7A7067; font-size: 12px; margin-bottom: 12px;">📍 台南市安平區永華三街333號6樓-3</div>
    <div style="display: flex; justify-content: flex-end; gap: 8px;">
      <span style="background-color: #F7EFE6; color: #8C6239; font-size: 11px; padding: 5px 10px; border-radius: 14px; border: 1px solid #E8DCB8;">AI重辨</span>
      <span style="background-color: #8C6239; color: #FFFFFF; font-size: 12px; padding: 6px 14px; border-radius: 16px; font-weight: bold;">⚡ 立即刊登</span>
    </div>
  </div>

  <!-- 刊登操作列兩行排版模擬 (問題1) -->
  <div style="background-color: #EDE6DB; padding: 12px; border-radius: 8px; border: 1px solid #DED4C5;">
    <div style="color: #2D2620; font-weight: bold; font-size: 13px; margin-bottom: 8px;">
      📦 準備刊登：羅技 M650 靜音無線藍牙滑鼠
    </div>
    <div style="display: flex; justify-content: space-between; align-items: center;">
      <span style="width: 60px;"></span> <!-- 平衡留白 -->
      <span style="background-color: #8C6239; color: #FFFFFF; font-size: 12px; padding: 6px 16px; border-radius: 16px; font-weight: bold;">⚡ 填入資料</span>
      <span style="color: #7A7067; font-size: 12px; padding: 6px 8px;">返回清單</span>
    </div>
  </div>

</div>

---

## 🍵 方案 B：極簡奶霜燕麥米 (Oatmeal Cream & Soft Khaki)

> **設計核心**：全體維持在同一明度與飽和度範圍內，按鈕與裝飾全採用溫潤的深奶茶色與燕麥米灰，風格極為低調、典雅與舒適。

### 1. 色票調色盤 (Color Palette)

<table>
  <tr>
    <th width="30%">角色名稱</th>
    <th width="25%">色碼 (Hex)</th>
    <th width="45%">顏色實體預覽</th>
  </tr>
  <tr>
    <td><b>全域底色 (Background)</b></td>
    <td><code>#FBF9F6</code></td>
    <td style="background-color: #FBF9F6; border: 1px solid #EAE4DD; padding: 12px; color: #362F29; font-weight: bold;">淺燕麥白 #FBF9F6</td>
  </tr>
  <tr>
    <td><b>頂部標題列 (AppBar)</b></td>
    <td><code>#F4EFEA</code></td>
    <td style="background-color: #F4EFEA; padding: 12px; color: #362F29; font-weight: bold;">柔和奶霜 #F4EFEA</td>
  </tr>
  <tr>
    <td><b>主操作按鈕 (Primary CTA)</b></td>
    <td><code>#D0C4B4</code></td>
    <td style="background-color: #D0C4B4; padding: 12px; color: #3E342B; font-weight: bold; border-radius: 6px;">深奶茶色 #D0C4B4 (文字 #3E342B)</td>
  </tr>
  <tr>
    <td><b>卡片狀態標籤 (Badge)</b></td>
    <td><code>#EFE8DE</code></td>
    <td style="background-color: #EFE8DE; padding: 12px; color: #5C4F43; font-weight: bold; border-radius: 4px;">燕麥狀態標籤 #EFE8DE (文字 #5C4F43)</td>
  </tr>
  <tr>
    <td><b>主要文字 (Text Primary)</b></td>
    <td><code>#362F29</code></td>
    <td style="background-color: #FFFFFF; padding: 12px; color: #362F29; font-weight: bold;">暖褐黑文字 #362F29</td>
  </tr>
  <tr>
    <td><b>卡片邊框與分隔線</b></td>
    <td><code>#EAE4DD</code></td>
    <td style="background-color: #EAE4DD; padding: 12px; color: #6E6359;">淺卡其灰邊框 #EAE4DD</td>
  </tr>
</table>

### 2. UI 介面模擬預覽（方案 B）

<div style="background-color: #FBF9F6; padding: 16px; border-radius: 12px; border: 1px solid #EAE4DD; max-width: 420px; font-family: sans-serif;">

  <!-- 頂部標題列 -->
  <div style="background-color: #F4EFEA; padding: 12px 16px; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #EAE4DD;">
    <span style="color: #362F29; font-weight: bold; font-size: 15px;">扶輪公益網物資助手</span>
    <span style="background-color: #E6DFD5; color: #4A3E35; font-size: 12px; padding: 4px 10px; border-radius: 12px;">🔄 刷新</span>
  </div>

  <!-- 分頁列 -->
  <div style="display: flex; justify-content: space-around; padding: 10px 0; border-bottom: 2px solid #F4EFEA; margin-bottom: 12px;">
    <span style="color: #6E5F52; font-weight: bold; border-bottom: 2px solid #6E5F52; padding-bottom: 4px;">物資佇列</span>
    <span style="color: #8C8074;">17rcn 刊登</span>
    <span style="color: #8C8074;">偏好設定</span>
  </div>

  <!-- 物資卡片模擬 -->
  <div style="background-color: #FFFFFF; padding: 14px; border-radius: 8px; border: 1px solid #EAE4DD; box-shadow: 0 2px 4px rgba(0,0,0,0.02); margin-bottom: 12px;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
      <span style="background-color: #EFE8DE; color: #5C4F43; font-size: 11px; padding: 3px 8px; border-radius: 4px; font-weight: bold;">待刊登</span>
      <span style="color: #8C8074; font-size: 12px;">電腦周邊 · 數量：1</span>
    </div>
    <div style="color: #362F29; font-weight: bold; font-size: 15px; margin-bottom: 6px;">羅技 M650 靜音無線藍牙滑鼠</div>
    <div style="color: #8C8074; font-size: 12px; margin-bottom: 12px;">📍 台南市安平區永華三街333號6樓-3</div>
    <div style="display: flex; justify-content: flex-end; gap: 8px;">
      <span style="background-color: #F7F3EE; color: #5C4F43; font-size: 11px; padding: 5px 10px; border-radius: 14px; border: 1px solid #EAE4DD;">AI重辨</span>
      <span style="background-color: #D0C4B4; color: #3E342B; font-size: 12px; padding: 6px 14px; border-radius: 16px; font-weight: bold;">⚡ 立即刊登</span>
    </div>
  </div>

  <!-- 刊登操作列兩行排版模擬 (問題1) -->
  <div style="background-color: #F4EFEA; padding: 12px; border-radius: 8px; border: 1px solid #E6DED5;">
    <div style="color: #362F29; font-weight: bold; font-size: 13px; margin-bottom: 8px;">
      📦 準備刊登：羅技 M650 靜音無線藍牙滑鼠
    </div>
    <div style="display: flex; justify-content: space-between; align-items: center;">
      <span style="width: 60px;"></span> <!-- 平衡留白 -->
      <span style="background-color: #D0C4B4; color: #3E342B; font-size: 12px; padding: 6px 16px; border-radius: 16px; font-weight: bold;">⚡ 填入資料</span>
      <span style="color: #8C8074; font-size: 12px; padding: 6px 8px;">返回清單</span>
    </div>
  </div>

</div>

---

## 💡 總結與建議

1. **若重視戶外操作與清晰度**：
   - 推薦 **方案 A（焦糖米）**，焦糖色按鈕在米白背景上有高對比度，大太陽或走動中一眼就能看清「⚡ 填入資料」按鈕。
2. **若重視柔和低負擔**：
   - 推薦 **方案 B（奶霜米）**，整體色調高度統一雅緻，久看不累。
