"""Compare collection-scan and indexed explain plans on the configured DB; no writes."""
import json
from pathlib import Path
from animal_shelter import AnimalShelter,rescue_query

def measure(repo):
    db=repo.collection.database
    out={}
    for label,hint in [('collection_scan',{'$natural':1}),('indexed','rescue_type_sex_age')]:
        plan=db.command('explain',{'find':repo.collection.name,'filter':rescue_query('water'),
            'hint':hint},verbosity='executionStats')
        s=plan['executionStats']
        out[label]={k:s[k] for k in ('nReturned','totalDocsExamined','totalKeysExamined','executionTimeMillis')}
    return out
if __name__=='__main__':
    repo=AnimalShelter.connect()
    try:
        report=measure(repo)
        Path('benchmark_results.json').write_text(json.dumps(report,indent=2))
        print(json.dumps(report,indent=2))
    finally:repo.close()
