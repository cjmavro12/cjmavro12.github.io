import argparse
import logging
from dash import Dash, Input, Output, State, dcc, html, dash_table
import dash_leaflet as dl
import plotly.graph_objects as go
from animal_shelter import AnimalShelter, DatabaseError, ValidationError, dashboard_data, map_record, FIELDS

def make_app(repo,demo=False):
    app=Dash(__name__)
    app.layout=html.Div([
        html.H1('Grazioso Salvare Rescue Dashboard'),html.P('CJ Mavrogeorge | CS 499 database enhancement'),
        html.P('DEMO — synthetic sample records; no live MongoDB connection' if demo else 'Connected to MongoDB'),
        dcc.RadioItems(id='category',options=[{'label':l,'value':v} for l,v in [('All animals','reset'),('Water rescue','water'),('Mountain rescue','mountain'),('Disaster rescue','disaster')]],value='reset',inline=True),
        dcc.Store(id='page',data=0),html.Button('Previous',id='previous'),html.Button('Next',id='next'),
        html.P(id='status'),
        dash_table.DataTable(id='animals',columns=[{'name':f.replace('_',' ').title(),'id':f} for f in FIELDS],
                            data=[],page_action='none',row_selectable='single',selected_row_ids=[],style_table={'overflowX':'auto'}),
        dcc.Graph(id='breeds'),html.Div(id='map'),
        html.P('Chart shows the top 20 breeds across the selected rescue filter. Table shows 10 records per page.')
    ],style={'maxWidth':'1200px','margin':'30px auto','fontFamily':'Arial'})
    @app.callback(Output('page','data'),Input('category','value'),Input('previous','n_clicks'),Input('next','n_clicks'),State('page','data'))
    def change_page(category,prev,nxt,page):
        from dash import ctx
        if ctx.triggered_id=='next':return min(page+1,10000)
        if ctx.triggered_id=='previous':return max(page-1,0)
        return 0
    @app.callback(Output('animals','data'),Output('animals','selected_row_ids'),Output('breeds','figure'),Output('status','children'),Output('previous','disabled'),Output('next','disabled'),Input('category','value'),Input('page','data'))
    def refresh(category,page):
        try:
            rows,pages,summary,total=dashboard_data(repo,category,page)
            chart=go.Figure(go.Pie(labels=[x['_id'] or 'Unknown' for x in summary],values=[x['count'] for x in summary]))
            chart.update_layout(title='Breed totals for selected filter')
            return rows,[],chart,f'{total} matching records | Page {page+1} of {max(pages,1)}',page==0,page+1>=pages
        except (DatabaseError,ValidationError) as e:
            return [],[],go.Figure(),str(e),True,True
    @app.callback(Output('map','children'),Input('animals','data'),Input('animals','selected_row_ids'))
    def show_map(rows,selected):
        point=map_record(rows,selected)
        if point is None:return html.P('No valid location for this selection.')
        pos=[point['lat'],point['lon']]
        return dl.Map([dl.TileLayer(),dl.Marker(position=pos,children=[dl.Tooltip(point['breed']),dl.Popup(point['name'])])],center=pos,zoom=11,style={'height':'400px'})
    return app

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--demo',action='store_true');args=parser.parse_args()
    logging.basicConfig(level=logging.INFO)
    if args.demo:
        import mongomock
        from demo_data import records
        repo=AnimalShelter(mongomock.MongoClient().demo.animals)
        for r in records():repo.create(r)
    else:repo=AnimalShelter.connect()
    try:make_app(repo,args.demo).run(host='127.0.0.1',port=8050,debug=False)
    finally:repo.close()
