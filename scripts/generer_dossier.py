"""Régénération facultative : Python/reportlab et Graphviz (dot), hors chaîne Maven."""
from pathlib import Path
from html import escape
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
import subprocess
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Image, PageBreak, Table, TableStyle, KeepTogether
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.pagesizes import landscape, A4
from reportlab.lib.utils import ImageReader
root = Path(__file__).resolve().parents[1]
docs = root / 'docs'
subprocess.run(['dot', '-Tpng', '-Gdpi=220', str(docs/'classes.dot'), '-o', str(docs/'classes.png')], check=True)
for name, filename in [('OpenSans', 'OpenSans-Regular.ttf'), ('OpenSansBold', 'OpenSans-Bold.ttf'), ('OpenSansItalic', 'OpenSans-Italic.ttf'), ('OpenSansBoldItalic', 'OpenSans-BoldItalic.ttf')]:
    pdfmetrics.registerFont(TTFont(name, '/usr/share/fonts/truetype/open-sans/' + filename))
pdfmetrics.registerFontFamily('OpenSans', normal='OpenSans', bold='OpenSansBold', italic='OpenSansItalic', boldItalic='OpenSansBoldItalic')
styles = getSampleStyleSheet()
for style in styles.byName.values(): style.fontName = 'OpenSans'
styles['Title'].fontName = 'OpenSansBold'
styles['Heading2'].fontName = 'OpenSansBold'
styles.add(ParagraphStyle(name='Texte', fontName='OpenSans', fontSize=9.5, leading=12, spaceAfter=7))
styles.add(ParagraphStyle(name='Petit', fontName='OpenSans', fontSize=8.6, leading=10.5))
styles['Title'].fontSize=19
styles['Title'].leading=23
styles['Heading2'].fontSize=11
styles['Heading2'].leading=14
story=[]
def p(s,style='Texte'): return Paragraph(s, styles[style])
story += [p('IMT Arena — Dossier de conception', 'Title'), p('1. Modèle objet et relations', 'Heading2')]
w,h=ImageReader(str(docs/'classes.png')).getSize();scale=min(762/w,365/h)
story.append(Image(str(docs/'classes.png'), width=w*scale,height=h*scale))
story += [Spacer(1,7),p('<b>Lecture UML.</b> Triangle vide : héritage (trait pointillé : réalisation) ; losange plein : composition. Multiplicités portées sur les associations. Classe et Qualifie sont des interfaces de capacités ; Jeu et Personne sont abstraites. Le diagramme Mermaid complet et éditable est fourni dans classes.mmd.', 'Petit'),
 p('<b>Invariants.</b> Une personne possède un id unique. Joueur et Coach ne peuvent rejoindre deux équipes ; les mutations passent par Equipe. La réservation fige les membres pendant le tournoi. Match valide adversaires distincts, effectifs et qualification ; une victoire donne trois points, un incident aucun. Après trois victoires depuis son arrivée, un amateur devient professionnel ; les victoires du coach restent acquises après transfert.', 'Petit'), PageBreak()]
story += [p('2. Collections, patterns et concurrence', 'Title')]
rows=[['Besoin / structure','Justification et précaution'],
['Tournois : PriorityQueue<Tournoi>','Comparable sur date puis nom immuables ; tête O(1), ajout/retrait O(log n). Liste affichée triée O(n log n), car itération de la file non triée.'],
['Registres : HashMap (personnes : LinkedHashMap)','Recherche moyenne O(1), rejet des doublons par nom ou id. LinkedHashMap préserve l’ordre des arbitres : tirage reproductible.'],
['Qualifications : Set<Jeu> immuable','Unicité, appartenance moyenne O(1). Jeux extensibles : EnumSet inadapté. Aucune modification externe.'],
['Effectifs / matchs : ArrayList','Ordre stable, accès O(1), ajout amorti O(1). Copies List.copyOf ; aucune liste interne exposée.'],
['Classement : List triée + Comparator','Points décroissants puis nom ; O(n log n). Points capturés avant tri. Pas de TreeSet indexé sur des points mutables.'],
['Gagnants : LinkedHashMap<Equipe,List<Match>>','Regroupement dans l’ordre du classement, listes immuables. Incidents exclus. Coût O(e*m), suffisant pour quelques équipes.']]
data=[[p(escape(x),'Petit') for x in row] for row in rows]
t=Table(data,colWidths=[220,542]); t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#dce8f6')),('VALIGN',(0,0),(-1,-1),'TOP'),('GRID',(0,0),(-1,-1),0.3,colors.HexColor('#a9b8ca')),('LEFTPADDING',(0,0),(-1,-1),6),('RIGHTPADDING',(0,0),(-1,-1),6),('TOPPADDING',(0,0),(-1,-1),4),('BOTTOMPADDING',(0,0),(-1,-1),4)]));story += [t,Spacer(1,9)]
story += [p('<b>Patterns utiles.</b> Strategy sépare le vainqueur du déroulement : hasard équitable, pondération par points ou meilleure équipe. Trois petites classes implémentent une interface avec des conditions simples. Observer diffuse résultat/incident vers des composants indépendants (exemple : CompteurMatchs) ; callbacks hors verrou, exceptions collectées. Ils doivent être rapides et coopératifs. Factory/Builder non retenus : constructeurs courts et validants suffisants ; pas de couche ajoutée sans besoin.'),
p('<b>Ordonnancement et sûreté.</b> Un ExecutorService fixe exécute au plus trois tournois. Le moniteur court d’Arena atomise priorité, capacité et lancement ; chaque équipe est réservée sous son propre moniteur, avec rollback en cas d’échec. Aucun verrou d’équipe ne couvre sommeil, callback ou JPA. Appartenances exclusives + effectifs figés empêchent la double mobilisation des joueurs/coachs. Les mises à jour de résultat et points locaux sont sérialisées par Tournoi ; consulter() retourne une photographie immuable. L’attente du CompletableFuture est bornée et couvre la sauvegarde. close() ferme les soumissions, attend, puis interrompt au besoin ; le dépôt ferme après les workers.'),
p('<b>Simulation et ambiguïtés.</b> Random privé et graine par tournoi ; rencontres et arbitres d’ordre stable. Arènes logiques par lots : durée M*t/A, dernier lot proratisé (15*100/3 = 500 ms), hors surcoûts CPU/JPA. Le sujet n’impose pas un planning exclusif des arbitres/équipes par match. Disponible signifie non engagé dans un tournoi ; arbitrés compte les matchs joués. Réussite : 10*joués >= 7*prévus. Vainqueur : points locaux puis nom ; aucun si zéro match joué.'),
p('<b>Persistance et validation.</b> Rapport est la seule entité, sans association vers le modèle vivant. DepotRapports découple JPA ; DepotJpa partage la factory et utilise un EntityManager par opération, transaction avec rollback. H2 est un fichier. Les tests JUnit couvrent règles, erreurs, hasard, concurrence, fermeture et réouverture du fichier ; le détail des résultats réellement exécutés figure dans VALIDATION.md.')]
def footer(c,d):
 c.setFont('OpenSans',8);c.setFillColor(colors.HexColor('#52647b'));c.drawString(40,20,'TP conception orientée objet • Java 21 • IMT Arena');c.drawRightString(802,20,f'{d.page} / 2')
SimpleDocTemplate(str(docs/'DOSSIER_CONCEPTION.pdf'),pagesize=landscape(A4),rightMargin=40,leftMargin=40,topMargin=25,bottomMargin=33).build(story,onFirstPage=footer,onLaterPages=footer)
