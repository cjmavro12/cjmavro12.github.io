"""Synthetic records, not the original AAC dataset."""
def records():
    breeds=['Labrador Retriever Mix','German Shepherd','Golden Retriever','Domestic Shorthair']
    for i in range(80):
        yield {'animal_id':f'DEMO{i:04d}','animal_type':'Cat' if i%4==3 else 'Dog',
               'breed':breeds[i%4],'name':f'Sample {i+1}',
               'sex_upon_outcome':'Intact Female' if i%4==0 else 'Intact Male',
               'age_upon_outcome_in_weeks':30+i,'location_lat':30.25+(i%10)*.01,
               'location_long':-97.75+(i%8)*.01}
