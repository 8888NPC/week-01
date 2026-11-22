const statuses = {
    TODO: { label: '待开始', metric: 'todo' },
    IN_PROGRESS: { label: '进行中', metric: 'doing' },
    DONE: { label: '已完成', metric: 'done' }
};
const byId = id => document.getElementById(id);
const rows = byId('task-rows');
const filter = byId('filter');
const dialog = byId('task-dialog');
const dialogContent = byId('dialog-content');
const refresh = byId('refresh-list');
const formatter = new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' });
let tasks = [];
let pendingDialogAction = false;
let detailRequest = 0;
let listRequest = 0;
let noticeTimeout;

function element(tag, className, text) {
    const node = document.createElement(tag);
    node.className = className;
    node.textContent = text;
    return node;
}

function action(label, className, onClick) {
    const button = element('button', className, label);
    button.type = 'button';
    button.addEventListener('click', onClick);
    return button;
}

async function request(path, method = 'GET', data) {
    const options = { method, cache: 'no-store' };
    if (data !== undefined) {
        options.headers = { 'Content-Type': 'application/json' };
        options.body = JSON.stringify(data);
    }
    const response = await fetch(path, options);
    if (response.status === 204) return;
    const body = await response.json().catch(() => null);
    if (!response.ok) {
        const fields = body?.errors && Object.values(body.errors);
        throw new Error(fields?.join('。') || body?.detail || `请求失败（HTTP ${response.status}）`);
    }
    return body;
}

function renderRows() {
    const displayed = tasks.filter(task => !filter.value || task.status === filter.value);
    const renderedRows = displayed.map(task => {
        const row = document.createElement('tr');
        const name = action(task.title, 'task-name', () => showDetails(task.id));
        row.insertCell().append(name, element('span', 'description', task.description || `任务 #${task.id}`));
        row.insertCell().append(element('span', `badge ${task.status}`, statuses[task.status].label));
        const actions = element('div', 'row-actions', '');
        const edit = action('编辑', 'text-button', () => showDetails(task.id, true));
        const remove = action('删除', 'text-button delete', () => showDelete(task));
        edit.setAttribute('aria-label', `编辑 ${task.title}`);
        remove.setAttribute('aria-label', `删除 ${task.title}`);
        actions.append(edit, remove);
        row.insertCell().append(actions);
        return row;
    });
    rows.replaceChildren(...renderedRows);
    const message = byId('table-message');
    message.textContent = filter.value ? '当前状态下没有任务。' : '还没有任务，点击「新建任务」开始。';
    message.hidden = displayed.length > 0;
    byId('visible-count').textContent = `显示 ${displayed.length} / ${tasks.length} 项`;
}

function renderProgress() {
    const counts = { TODO: 0, IN_PROGRESS: 0, DONE: 0 };
    for (const task of tasks) counts[task.status]++;
    for (const [status, item] of Object.entries(statuses)) {
        byId(item.metric).textContent = counts[status];
    }
    const total = tasks.length;
    const done = counts.DONE;
    const progress = total ? Math.round(done / total * 100) : 0;
    byId('total').textContent = total;
    byId('progress-percent').textContent = `${progress}%`;
    document.querySelector('.ring').style.setProperty('--progress', `${progress}%`);
    byId('progress-caption').textContent = `已完成 ${done} / ${total} 项任务`;
    byId('progress-done').textContent = done;
    byId('progress-open').textContent = total - done;
}

function notify(message) {
    const notice = byId('notice');
    window.clearTimeout(noticeTimeout);
    notice.textContent = message;
    notice.hidden = false;
    noticeTimeout = window.setTimeout(() => { notice.hidden = true; }, 4000);
}

async function loadTasks() {
    const generation = ++listRequest;
    refresh.disabled = true;
    filter.disabled = true;
    try {
        const result = await request('/api/tasks');
        if (generation !== listRequest) return false;
        tasks = result;
        renderRows();
        renderProgress();
        return true;
    } catch (error) {
        if (generation !== listRequest) return false;
        const message = byId('table-message');
        message.textContent = `无法刷新任务列表。 ${error.message}`;
        message.hidden = false;
        return false;
    } finally {
        if (generation === listRequest) {
            refresh.disabled = false;
            filter.disabled = false;
        }
    }
}

async function checkHealth() {
    const indicator = byId('health');
    try {
        const { status } = await request('/api/health');
        if (status !== 'UP') throw new Error();
        indicator.className = 'health online';
        indicator.textContent = '服务在线';
    } catch {
        indicator.className = 'health offline';
        indicator.textContent = '服务不可用';
    }
}

function openDialog(title, content) {
    byId('dialog-title').textContent = title;
    dialogContent.replaceChildren(content);
    if (!dialog.open) dialog.showModal();
}

function closeDialog() {
    if (!pendingDialogAction) {
        detailRequest++;
        dialog.close();
    }
}

function bindMutation(container, buttons, onSave, successMessage) {
    const errorMessage = container.querySelector('.form-error');
    return async () => {
        if (pendingDialogAction) return;
        pendingDialogAction = true;
        buttons.forEach(button => { button.disabled = true; });
        errorMessage.hidden = true;
        try {
            await onSave();
            pendingDialogAction = false;
            closeDialog();
            const loaded = await loadTasks();
            notify(loaded ? successMessage : `${successMessage}，请手动刷新列表`);
        } catch (error) {
            errorMessage.textContent = error.message;
            errorMessage.hidden = false;
        } finally {
            pendingDialogAction = false;
            buttons.forEach(button => { button.disabled = false; });
        }
    };
}

function showForm(task) {
    const form = byId('task-form-template').content.firstElementChild.cloneNode(true);
    if (task) {
        for (const name of ['title', 'description', 'status']) {
            form.elements.namedItem(name).value = task[name];
        }
    }
    form.querySelector('[data-close]').addEventListener('click', closeDialog);
    const save = bindMutation(form, form.querySelectorAll('button'), () => {
        const data = Object.fromEntries(new FormData(form));
        if (!data.title.trim()) throw new Error('请输入任务名称');
        return task ? request(`/api/tasks/${task.id}`, 'PUT', data) : request('/api/tasks', 'POST', data);
    }, task ? '任务已更新' : '任务已创建');
    form.addEventListener('submit', event => {
        event.preventDefault();
        save();
    });
    openDialog(task ? '编辑任务' : '新建任务', form);
    form.elements.namedItem('title').focus();
}

async function showDetails(id, edit = false) {
    const generation = ++detailRequest;
    openDialog(edit ? '编辑任务' : '任务详情', element('p', 'dialog-text', '正在读取任务…'));
    try {
        const task = await request(`/api/tasks/${id}`);
        if (generation !== detailRequest) return;
        if (edit) {
            showForm(task);
            return;
        }
        const content = document.createElement('div');
        const details = element('dl', 'detail-list', '');
        const fields = [
            ['编号', `#${task.id}`],
            ['状态', statuses[task.status].label],
            ['描述', task.description || '暂无描述'],
            ['创建时间', formatter.format(new Date(task.createdAt))],
            ['更新时间', formatter.format(new Date(task.updatedAt))]
        ];
        for (const [label, value] of fields) {
            details.append(element('dt', '', label), element('dd', '', value));
        }
        const actions = element('div', 'dialog-actions', '');
        actions.append(action('关闭', 'secondary', closeDialog), action('编辑任务', 'primary', () => showForm(task)));
        content.append(details, actions);
        openDialog(task.title, content);
    } catch (error) {
        if (generation === detailRequest) {
            openDialog('无法读取任务', element('p', 'dialog-text', error.message));
        }
    }
}

function showDelete(task) {
    const content = document.createElement('div');
    const error = element('p', 'form-error', '');
    error.hidden = true;
    error.setAttribute('role', 'alert');
    const actions = element('div', 'dialog-actions', '');
    const cancel = action('取消', 'secondary', closeDialog);
    const remove = action('确认删除', 'danger', () => removeTask());
    actions.append(cancel, remove);
    content.append(element('p', 'dialog-text', `确定删除「${task.title}」？删除后无法恢复。`), error, actions);
    const removeTask = bindMutation(content, [cancel, remove], () => request(`/api/tasks/${task.id}`, 'DELETE'), '任务已删除');
    openDialog('删除任务', content);
    cancel.focus();
}

byId('new-task').addEventListener('click', () => showForm());
byId('close-dialog').addEventListener('click', closeDialog);
dialog.addEventListener('cancel', event => {
    event.preventDefault();
    closeDialog();
});
refresh.addEventListener('click', () => {
    loadTasks();
    checkHealth();
});
filter.addEventListener('change', renderRows);

const navLinks = [...document.querySelectorAll('.nav-link')];
function updateNavigation() {
    const target = location.hash || '#overview';
    for (const link of navLinks) {
        const selected = link.hash === target;
        link.classList.toggle('selected', selected);
        if (selected) link.setAttribute('aria-current', 'location');
        else link.removeAttribute('aria-current');
    }
}
window.addEventListener('hashchange', updateNavigation);
updateNavigation();
loadTasks();
checkHealth();
