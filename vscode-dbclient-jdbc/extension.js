const {platform} = require("os");
const { exec } = require("child_process")

function activate(context) {
    const filterCmd = platform() === 'win32' ? 'findstr' : 'grep';
    exec(`jps|${filterCmd} jdbc-adapter-server`, (err, stdout) => {
        const pid = stdout?.match(/\d+/)?.[0]
        if (pid) {
            process.kill(pid)
        }
    })
}

function deactivate() {
}

module.exports = {activate, deactivate}
