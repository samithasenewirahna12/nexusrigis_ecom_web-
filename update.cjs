const fs = require('fs');
const file = 'c:/Users/samitha senevirathna/Desktop/e-commerce platform/frontend/src/views/admin/ManageDiscounts.vue';
let content = fs.readFileSync(file, 'utf8');

// Replace saveDeal
const startIdx = content.indexOf('const saveDeal = async () => {');
const endStr = '    showFormModal.value = false';
const endIdx = content.indexOf(endStr, startIdx);

if (startIdx !== -1 && endIdx !== -1) {
    const oldBlock = content.substring(startIdx, endIdx + endStr.length);
    const newBlock = `const saveDeal = async () => {
  if (form.value.applyType === 'single' && !form.value.productId) { loadError.value = 'Product is required.'; return }
  if (form.value.applyType === 'multiple' && (!form.value.productIds || form.value.productIds.length === 0)) { loadError.value = 'Please select at least one product.'; return }
  if (form.value.applyType === 'category' && !form.value.categoryId) { loadError.value = 'Category is required.'; return }
  if (form.value.discount < 1 || form.value.discount > 100) { loadError.value = 'Discount must be between 1% and 100%.'; return }
  if (!form.value.startDate) { loadError.value = 'Start date is required.'; return }
  if (!form.value.endDate) { loadError.value = 'End date is required.'; return }
  if (form.value.endDate < form.value.startDate) { loadError.value = 'End date cannot be before start date.'; return }

  isSaving.value = true
  loadError.value = ''
  
  const payload = { 
    productId: form.value.applyType === 'single' ? form.value.productId : undefined,
    productIds: form.value.applyType === 'multiple' ? form.value.productIds : undefined,
    categoryId: form.value.applyType === 'category' ? form.value.categoryId : undefined,
    discountPercentage: form.value.discount, 
    badgeText: form.value.badgeText, 
    startDate: form.value.startDate, 
    endDate: form.value.endDate 
  }
  
  try {
    if (editingDealId.value) {
      const { data } = await api.put(\`/deals/\${editingDealId.value}\`, payload)
      const index = deals.value.findIndex(deal => deal.id === editingDealId.value)
      if (index >= 0) deals.value[index] = normalizeDeal(data ?? { ...payload, dealId: editingDealId.value })
      successMessage.value = 'Discount updated successfully.'
    } else {
      const { data } = await api.post('/deals/bulk', payload)
      if (Array.isArray(data)) {
        deals.value = [...data.map(normalizeDeal), ...deals.value]
      } else {
        deals.value.unshift(normalizeDeal(data ?? { ...payload, dealId: \`LOCAL-\${Date.now()}\` }))
      }
      successMessage.value = 'Discount(s) created successfully.'
    }
    showFormModal.value = false`;
    content = content.replace(oldBlock, newBlock);
}

// Update UI
const oldUIStart = content.indexOf('<form @submit.prevent="saveDeal">');
const oldUIEnd = content.indexOf('<!-- Discount Amount -->');
if (oldUIStart !== -1 && oldUIEnd !== -1) {
    const oldUIBlock = content.substring(oldUIStart, oldUIEnd);
    const newUIBlock = `<form @submit.prevent="saveDeal">
              <div class="mb-5 space-y-4">
                
                <div v-if="!editingDealId" class="flex flex-col gap-1.5">
                  <label class="text-xs font-bold text-slate-700">Apply To</label>
                  <div class="flex gap-4">
                    <label class="flex items-center gap-2"><input type="radio" v-model="form.applyType" value="single" /> Single Product</label>
                    <label class="flex items-center gap-2"><input type="radio" v-model="form.applyType" value="multiple" /> Multiple Products</label>
                    <label class="flex items-center gap-2"><input type="radio" v-model="form.applyType" value="category" /> Category</label>
                  </div>
                </div>

                <div v-if="form.applyType === 'single' || editingDealId" class="flex flex-col gap-1.5">
                  <label class="text-xs font-bold text-slate-700">Select Product *</label>
                  <select v-model="form.productId" :disabled="!!editingDealId" class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10 disabled:opacity-50">
                    <option value="" disabled>-- Select a product --</option>
                    <option v-for="product in products" :key="product.productId || product.id" :value="product.productId || product.id">
                      {{ product.name || product.productName }}
                    </option>
                  </select>
                </div>
                
                <div v-if="form.applyType === 'multiple' && !editingDealId" class="flex flex-col gap-1.5">
                  <label class="text-xs font-bold text-slate-700">Select Products * (Hold Ctrl/Cmd to select multiple)</label>
                  <select v-model="form.productIds" multiple class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10 h-32">
                    <option v-for="product in products" :key="product.productId || product.id" :value="product.productId || product.id">
                      {{ product.name || product.productName }}
                    </option>
                  </select>
                </div>

                <div v-if="form.applyType === 'category' && !editingDealId" class="flex flex-col gap-1.5">
                  <label class="text-xs font-bold text-slate-700">Select Category *</label>
                  <select v-model="form.categoryId" class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10">
                    <option value="" disabled>-- Select a category --</option>
                    <option v-for="cat in categories" :key="cat.categoryId || cat.id" :value="cat.categoryId || cat.id">
                      {{ cat.name || cat.categoryName }}
                    </option>
                  </select>
                </div>
                
                `;
    content = content.replace(oldUIBlock, newUIBlock);
}
fs.writeFileSync(file, content);
console.log('done');
