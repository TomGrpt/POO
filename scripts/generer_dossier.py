from pathlib import Path
from html import escape
import subprocess
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Image, PageBreak, Table, TableStyle
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.pagesizes import landscape, A4
from reportlab.lib.utils import ImageReader

racine = Path(__file__).resolve().parents[1]
docs = racine / 'docs'
subprocess.run(['dot', '-Tpng', '-Gdpi=220', str(docs / 'classes.dot'), '-o', str(docs / 'classes.png')], check=True)
for nom, fichier in [('OpenSans', 'OpenSans-Regular.ttf'), ('OpenSansBold', 'OpenSans-Bold.ttf')]:
    pdfmetrics.registerFont(TTFont(nom, '/usr/share/fonts/truetype/open-sans/' + fichier))
pdfmetrics.registerFontFamily('OpenSans', normal='OpenSans', bold='OpenSansBold')
styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name='Texte', fontName='OpenSans', fontSize=9, leading=12, spaceAfter=7))
styles.add(ParagraphStyle(name='Petit', fontName='OpenSans', fontSize=8.5, leading=11))
styles['Title'].fontName = 'OpenSansBold'
styles['Title'].fontSize = 19
styles['Title'].leading = 23

def paragraphe(texte, style='Texte'):
    return Paragraph(texte, styles[style])

contenu = [paragraphe('IMT Arena — conception et organisation', 'Title')]
largeur, hauteur = ImageReader(str(docs / 'classes.png')).getSize()
rapport = min(760 / largeur, 365 / hauteur)
contenu.append(Image(str(docs / 'classes.png'), width=largeur * rapport, height=hauteur * rapport))
contenu += [Spacer(1, 8), paragraphe('<b>Lecture.</b> Triangle vide : héritage ; trait pointillé : réalisation d’interface ; losange plein : composition. Les préfixes correspondent aux dossiers Java. Le diagramme Mermaid fournit aussi les interfaces, énumérations, Verrou et Textes.', 'Petit'), paragraphe('<b>Packages.</b> application : démonstration ; metier : participants, équipes, tournois et coordination ; jeux : sélection ; strategies : victoire ; persistance : rapports et JPA ; util : textes. Les classes métier restent ensemble afin de conserver les modifications internes sans ajouter de setters publics.', 'Petit'), paragraphe('<b>Règles.</b> Un joueur ou coach ne rejoint qu’une équipe ; ajouter un membre déjà ailleurs réalise un transfert si les équipes sont libres. Trois victoires depuis l’arrivée rendent le joueur professionnel. Le coach garde son total de carrière. Une victoire rapporte trois points ; un incident ne rapporte rien. Les rapports sont les seuls objets persistés.', 'Petit'), PageBreak()]
contenu.append(paragraphe('Collections, Strategy et synchronisation', 'Title'))
lignes = [
    ['Besoin / structure', 'Justification'],
    ['Registres : HashMap', 'Accès moyen O(1) par nom ou identifiant ; doublons refusés à l’enregistrement.'],
    ['Planification : PriorityQueue', 'Tournoi Comparable par date puis nom immuables. Tête O(1), ajout/retrait O(log n). La liste affichée est une copie triée.'],
    ['Effectifs et résultats : ArrayList', 'Ordre conservé et parcours simple. Les listes internes sont protégées par des copies non modifiables.'],
    ['Qualifications : HashSet', 'Absence de doublon ; recherche moyenne O(1). Les jeux restent des classes extensibles.'],
    ['Classements : List et Comparator', 'Tri O(n log n), points ou budget décroissants puis nom. Liste recréée à chaque consultation, sous verrou ; pas d’arbre conservé sur des scores modifiables.'],
    ['Groupes : LinkedHashMap', 'Une équipe gagnante associée à ses matchs. Ordre de première victoire conservé. Listes copiées et map non modifiable.']
]
tableau = Table([[paragraphe(escape(cellule), 'Petit') for cellule in ligne] for ligne in lignes], colWidths=[225, 535])
tableau.setStyle(TableStyle([('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#dce8f6')), ('GRID', (0, 0), (-1, -1), 0.3, colors.HexColor('#a9b8ca')), ('VALIGN', (0, 0), (-1, -1), 'TOP'), ('TOPPADDING', (0, 0), (-1, -1), 5), ('BOTTOMPADDING', (0, 0), (-1, -1), 5)]))
contenu += [tableau, Spacer(1, 10), paragraphe('<b>Strategy.</b> StrategieVictoire définit choisir. TirageEquitable utilise le hasard ; MeilleureEquipe compare les points puis le nom. Modifier l’objet stratégie change la règle sans modifier Match. Aucun autre pattern n’est ajouté sans besoin.'), paragraphe('<b>Concurrence.</b> Une FutureTask par tournoi est exécutée directement, dans un Thread dédié ou dans le pool de trois workers. Le compteur enCours contrôle aussi les admissions. Verrou.OBJET protège transferts, réservations et publication finale. Les pauses, stratégies et attentes du futur sont hors verrou. Les résultats sont calculés localement, puis points, rapport, état et libérations sont publiés à la fin. La sauvegarde JPA reste sous verrou pour simplifier la cohérence, au prix d’un ralentissement possible des autres opérations protégées.'), paragraphe('<b>Temps et hasard.</b> Chaque tournoi possède son Random et sa graine. Deux boucles i puis j=i+1 créent toutes les paires. Cadence : temps par match / arènes, sans thread de match. Durée cible M*t/A, hors surcoûts ; les arènes sont un débit logique, pas un planning sportif. Un arbitre disponible est réservé à chaque tournoi. Réussite : 10*joués >= 7*prévus. Vainqueur : points locaux puis nom ; « Aucun » si zéro match joué.'), paragraphe('<b>Persistance et arrêt.</b> RapportTournoi est une entité JPA ; Hibernate enregistre dans H2 fichier. Un EntityManager par opération et une transaction par sauvegarde. Une erreur remonte via Future.get, sans garder les réservations. close attend les tâches, joint les threads dédiés et ferme le pool ; une interruption demande l’arrêt des workers possédés. Les extensions doivent coopérer. Le gestionnaire ferme avant le dépôt. Les tests vérifient les règles, la concurrence et la relecture après réouverture.')]

def pied(canvas, document):
    canvas.setFont('OpenSans', 8)
    canvas.drawString(40, 19, 'Java 17 — version réorganisée')
    canvas.drawRightString(802, 19, str(document.page) + ' / 2')

SimpleDocTemplate(str(racine / 'Dossier_conception.pdf'), pagesize=landscape(A4), leftMargin=40, rightMargin=40, topMargin=25, bottomMargin=33).build(contenu, onFirstPage=pied, onLaterPages=pied)
