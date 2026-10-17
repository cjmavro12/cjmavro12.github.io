from animal_shelter import AnimalShelter
if __name__=='__main__':
    repo=AnimalShelter.connect()
    try:print(repo.create_indexes())
    finally:repo.close()
