from pathlib import Path
import subprocess

workflow = Path('.github/workflows/apply-v114.yml')
lines = workflow.read_text(encoding='utf-8').splitlines()
start = next(i for i, x in enumerate(lines) if "python - <<'PY'" in x) + 1
end = next(i for i in range(start, len(lines)) if lines[i].strip() == 'PY')
out = []
for line in lines[start:end]:
    out.append(line[10:] if line.startswith('          ') else line)
patch = Path('/tmp/apply_v114.py')
patch.write_text('\n'.join(out) + '\n', encoding='utf-8')
subprocess.run(['python', str(patch)], check=True)

scheduler = Path('app/src/main/java/com/example/creditcalculator/ReminderScheduler.java')
s = scheduler.read_text(encoding='utf-8')
old = '''    public static double depositExpectedIncome(PaymentReminder r){if(r==null||!TYPE_DEPOSIT.equals(normalizeType(r.type)))return 0;return Math.max(0,r.principal*r.annualRate/100d*(r.months/12d));}\n    public static double depositFinalAmount(PaymentReminder r){return r==null?0:Math.max(0,r.principal+depositExpectedIncome(r));}\n'''
if old in s:
    s = s.replace(old, '', 1)
method = 'public static double depositExpectedIncome(PaymentReminder r)'
if s.count(method) != 1:
    raise RuntimeError(f'Expected one depositExpectedIncome method, found {s.count(method)}')
scheduler.write_text(s, encoding='utf-8')
